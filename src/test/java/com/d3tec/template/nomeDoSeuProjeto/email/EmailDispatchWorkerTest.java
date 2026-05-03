package com.d3tec.template.nomeDoSeuProjeto.email;

import com.d3tec.template.nomeDoSeuProjeto.email.config.EmailQueueProperties;
import com.d3tec.template.nomeDoSeuProjeto.email.config.MailProperties;
import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailJob;
import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailJobClaim;
import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailJobStatus;
import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailType;
import com.d3tec.template.nomeDoSeuProjeto.email.model.TransactionalEmail;
import com.d3tec.template.nomeDoSeuProjeto.email.queue.EmailQueuePort;
import com.d3tec.template.nomeDoSeuProjeto.email.sender.EmailSender;
import com.d3tec.template.nomeDoSeuProjeto.email.service.EmailDispatchWorker;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailDispatchWorkerTest {

    @Test
    void workerShouldRetryFailedJobs() {
        EmailQueuePort queuePort = mock(EmailQueuePort.class);
        EmailSender emailSender = mock(EmailSender.class);
        EmailQueueProperties queueProperties = new EmailQueueProperties();
        queueProperties.setBatchSize(1);
        queueProperties.setInitialBackoffSeconds(30);
        queueProperties.setMaxAttempts(5);
        MailProperties mailProperties = new MailProperties();
        mailProperties.getRateLimit().setMinIntervalMs(0);
        TaskExecutor taskExecutor = Runnable::run;

        EmailDispatchWorker worker = new EmailDispatchWorker(
                queuePort,
                emailSender,
                queueProperties,
                mailProperties,
                taskExecutor
        );

        EmailJobClaim claim = EmailJobClaim.builder()
                .claimToken("claim")
                .job(EmailJob.builder()
                        .id("job-1")
                        .attempt(1)
                        .createdAt(Instant.now())
                        .status(EmailJobStatus.PROCESSING)
                        .email(TransactionalEmail.builder()
                                .to("user@example.com")
                                .subject("subject")
                                .textBody("text")
                                .htmlBody("<p>text</p>")
                                .type(EmailType.EMAIL_VERIFICATION)
                                .idempotencyKey("idempotency")
                                .build())
                        .build())
                .build();

        when(queuePort.claimNext()).thenReturn(Optional.of(claim), Optional.empty());
        doThrow(new IllegalStateException("smtp down")).when(emailSender).send(any(TransactionalEmail.class));

        worker.pollQueue();

        verify(queuePort).retry(claim, Duration.ofSeconds(30), "smtp down");
        verify(queuePort, never()).moveToDeadLetter(any(EmailJobClaim.class), anyString());
    }
}
