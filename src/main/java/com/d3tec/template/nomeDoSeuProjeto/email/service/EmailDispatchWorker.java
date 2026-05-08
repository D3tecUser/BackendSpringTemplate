package com.d3tec.template.nomeDoSeuProjeto.email.service;

import com.d3tec.template.nomeDoSeuProjeto.email.config.EmailQueueProperties;
import com.d3tec.template.nomeDoSeuProjeto.email.config.MailProperties;
import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailJobClaim;
import com.d3tec.template.nomeDoSeuProjeto.email.queue.EmailQueuePort;
import com.d3tec.template.nomeDoSeuProjeto.email.sender.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class EmailDispatchWorker {

    private final EmailQueuePort emailQueuePort;
    private final EmailSender emailSender;
    private final EmailQueueProperties queueProperties;
    private final MailProperties mailProperties;
    @Qualifier("emailQueueExecutor")
    private final TaskExecutor taskExecutor;

    private final AtomicLong nextAllowedSendAt = new AtomicLong(0);
    private final AtomicBoolean queueStoppedByRedisFailure = new AtomicBoolean(false);
    private final AtomicBoolean smtpUnavailableLogged = new AtomicBoolean(false);

    public EmailDispatchWorker(
            EmailQueuePort emailQueuePort,
            EmailSender emailSender,
            EmailQueueProperties queueProperties,
            MailProperties mailProperties,
            @Qualifier("emailQueueExecutor") TaskExecutor taskExecutor
    ) {
        this.emailQueuePort = emailQueuePort;
        this.emailSender = emailSender;
        this.queueProperties = queueProperties;
        this.mailProperties = mailProperties;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(fixedDelayString = "${app.email.queue.poll-delay-ms:2000}")
    public void pollQueue() {
        if (!queueProperties.isEnabled() || !queueProperties.isWorkerEnabled() || !mailProperties.isEnabled()) {
            return;
        }
        if (queueStoppedByRedisFailure.get()) {
            return;
        }

        try {
            for (int i = 0; i < queueProperties.getBatchSize(); i++) {
                Optional<EmailJobClaim> claim = emailQueuePort.claimNext();
                if (claim.isEmpty()) {
                    return;
                }

                taskExecutor.execute(() -> processClaim(claim.get()));
            }
        } catch (RedisConnectionFailureException ex) {
            stopQueueAfterRedisFailure(ex);
        } catch (Exception ex) {
            log.error("Falha inesperada ao consumir fila de email: {}", summarize(ex));
        }
    }

    @Scheduled(fixedDelayString = "${app.email.queue.stale-job-requeue-delay-ms:30000}")
    public void requeueStaleJobs() {
        if (!queueProperties.isEnabled() || !queueProperties.isWorkerEnabled()) {
            return;
        }
        if (queueStoppedByRedisFailure.get()) {
            return;
        }

        try {
            int requeued = emailQueuePort.requeueStaleJobs();
            if (requeued > 0) {
                log.warn("Jobs de email reencaminhados por timeout de processamento: {}", requeued);
            }
        } catch (RedisConnectionFailureException ex) {
            stopQueueAfterRedisFailure(ex);
        } catch (Exception ex) {
            log.error("Falha inesperada ao reprocessar jobs stale: {}", summarize(ex));
        }
    }

    private void processClaim(EmailJobClaim claim) {
        try {
            throttle();
            emailSender.send(claim.getJob().getEmail());
            if (smtpUnavailableLogged.compareAndSet(true, false)) {
                log.info("SMTP voltou a responder; fila de email segue normalmente.");
            }
            emailQueuePort.acknowledge(claim);
            log.info("Email enviado: jobId={}, tipo={}, tentativa={}",
                    claim.getJob().getId(),
                    claim.getJob().getEmail().getType(),
                    claim.getJob().getAttempt());
        } catch (Exception ex) {
            int attempt = claim.getJob().getAttempt();
            boolean smtpFailure = isSmtpConnectionFailure(ex);
            String error = smtpFailure ? "SMTP indisponivel" : summarize(ex);

            if (attempt >= queueProperties.getMaxAttempts()) {
                emailQueuePort.moveToDeadLetter(claim, error);
                if (smtpFailure) {
                    log.warn("SMTP indisponivel; email movido para dead-letter: jobId={}, tentativa={}",
                            claim.getJob().getId(), attempt);
                } else {
                    log.error("Email movido para dead-letter: jobId={}, tentativa={}, erro={}",
                            claim.getJob().getId(), attempt, error);
                }
                return;
            }

            Duration backoff = calculateBackoff(attempt);
            emailQueuePort.retry(claim, backoff, error);
            if (smtpFailure) {
                if (smtpUnavailableLogged.compareAndSet(false, true)) {
                    log.warn("SMTP indisponivel; emails ficaram em retry. Quando o SMTP voltar, o envio sera retomado.");
                }
            } else {
                log.warn("Falha ao enviar email, reagendado para retry: jobId={}, tentativa={}, delay={}s, erro={}",
                        claim.getJob().getId(), attempt, backoff.toSeconds(), error);
            }
        }
    }

    private Duration calculateBackoff(int attempt) {
        long multiplier = 1L << Math.max(0, attempt - 1);
        long seconds = queueProperties.getInitialBackoffSeconds() * multiplier;
        return Duration.ofSeconds(Math.min(seconds, queueProperties.getMaxBackoffSeconds()));
    }

    private void throttle() throws InterruptedException {
        long minIntervalMs = mailProperties.getRateLimit().getMinIntervalMs();
        if (minIntervalMs <= 0) {
            return;
        }

        while (true) {
            long now = Instant.now().toEpochMilli();
            long nextAt = nextAllowedSendAt.get();
            if (nextAt <= now) {
                if (nextAllowedSendAt.compareAndSet(nextAt, now + minIntervalMs)) {
                    return;
                }
                continue;
            }

            Thread.sleep(nextAt - now);
        }
    }

    private String summarize(Throwable throwable) {
        String msg = throwable.getMessage();
        if (msg == null || msg.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return msg;
    }

    private void stopQueueAfterRedisFailure(RedisConnectionFailureException ex) {
        if (queueStoppedByRedisFailure.compareAndSet(false, true)) {
            log.warn("Fila de email pausada apos falha de conexao com Redis: {}. Reinicie a aplicacao apos normalizar o Redis.", summarize(ex));
        }
    }

    private boolean isSmtpConnectionFailure(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof ConnectException) {
                return true;
            }
            String className = cursor.getClass().getName();
            if (className.contains("MailConnectException")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
