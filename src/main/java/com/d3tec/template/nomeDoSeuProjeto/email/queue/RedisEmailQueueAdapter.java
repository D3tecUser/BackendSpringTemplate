package com.d3tec.template.nomeDoSeuProjeto.email.queue;

import com.d3tec.template.nomeDoSeuProjeto.email.config.EmailQueueProperties;
import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailJob;
import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailJobClaim;
import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailJobStatus;
import com.d3tec.template.nomeDoSeuProjeto.email.model.TransactionalEmail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisEmailQueueAdapter implements EmailQueuePort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new  ObjectMapper();
    private final EmailQueueProperties queueProperties;

    @Override
    public void enqueue(TransactionalEmail email) {
        String jobId = UUID.randomUUID().toString();
        String idempotencyKey = idempotencyKey(email.getIdempotencyKey());

        Boolean inserted = redisTemplate.opsForValue().setIfAbsent(
                idempotencyKey,
                jobId,
                Duration.ofHours(queueProperties.getIdempotencyTtlHours())
        );

        if (Boolean.FALSE.equals(inserted)) {
            return;
        }

        redisTemplate.opsForHash().put(payloadKey(), jobId, toJson(email));
        redisTemplate.opsForHash().put(createdAtKey(), jobId, String.valueOf(Instant.now().toEpochMilli()));
        redisTemplate.opsForZSet().add(pendingKey(), jobId, Instant.now().toEpochMilli());
    }

    @Override
    public Optional<EmailJobClaim> claimNext() {
        Instant now = Instant.now();
        String claimToken = UUID.randomUUID().toString();

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) redisTemplate.execute(new DefaultRedisScript<>(claimScript(), List.class),
                List.of(pendingKey(), processingKey(), claimKey(), attemptsKey()),
                String.valueOf(now.toEpochMilli()),
                String.valueOf(now.plusSeconds(queueProperties.getClaimTtlSeconds()).toEpochMilli()),
                claimToken);

        if (result == null || result.isEmpty() || result.get(0) == null) {
            return Optional.empty();
        }

        String jobId = result.get(0);
        int attempt = Integer.parseInt(result.get(1));

        Object rawPayload = redisTemplate.opsForHash().get(payloadKey(), jobId);
        Object rawCreatedAt = redisTemplate.opsForHash().get(createdAtKey(), jobId);
        if (rawPayload == null || rawCreatedAt == null) {
            acknowledgeMissingPayload(jobId, claimToken);
            return Optional.empty();
        }

        TransactionalEmail email = fromJson(String.valueOf(rawPayload));
        EmailJob job = EmailJob.builder()
                .id(jobId)
                .email(email)
                .attempt(attempt)
                .createdAt(Instant.ofEpochMilli(Long.parseLong(String.valueOf(rawCreatedAt))))
                .status(EmailJobStatus.PROCESSING)
                .build();

        return Optional.of(EmailJobClaim.builder()
                .job(job)
                .claimToken(claimToken)
                .build());
    }

    @Override
    public void acknowledge(EmailJobClaim claim) {
        redisTemplate.execute(new DefaultRedisScript<>(ackScript(), Long.class),
                List.of(processingKey(), claimKey(), payloadKey(), createdAtKey(), attemptsKey(), errorKey()),
                claim.getJob().getId(),
                claim.getClaimToken());
    }

    @Override
    public void retry(EmailJobClaim claim, Duration delay, String errorMessage) {
        redisTemplate.opsForHash().put(errorKey(), claim.getJob().getId(), sanitize(errorMessage));
        redisTemplate.execute(new DefaultRedisScript<>(retryScript(), Long.class),
                List.of(processingKey(), pendingKey(), claimKey()),
                claim.getJob().getId(),
                claim.getClaimToken(),
                String.valueOf(Instant.now().plus(delay).toEpochMilli()));
    }

    @Override
    public void moveToDeadLetter(EmailJobClaim claim, String errorMessage) {
        redisTemplate.opsForHash().put(errorKey(), claim.getJob().getId(), sanitize(errorMessage));
        redisTemplate.execute(new DefaultRedisScript<>(deadLetterScript(), Long.class),
                List.of(processingKey(), deadLetterKey(), claimKey()),
                claim.getJob().getId(),
                claim.getClaimToken(),
                String.valueOf(Instant.now().toEpochMilli()));
    }

    @Override
    public int requeueStaleJobs() {
        @SuppressWarnings("unchecked")
        List<String> jobIds = (List<String>) redisTemplate.execute(new DefaultRedisScript<>(requeueStaleScript(), List.class),
                List.of(processingKey(), pendingKey(), claimKey()),
                String.valueOf(Instant.now().toEpochMilli()),
                String.valueOf(Instant.now().toEpochMilli()));
        return jobIds == null ? 0 : jobIds.size();
    }

    private void acknowledgeMissingPayload(String jobId, String claimToken) {
        redisTemplate.execute(new DefaultRedisScript<>(ackScript(), Long.class),
                List.of(processingKey(), claimKey(), payloadKey(), createdAtKey(), attemptsKey(), errorKey()),
                jobId,
                claimToken);
    }

    private String toJson(TransactionalEmail email) {
        try {
            return objectMapper.writeValueAsString(email);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar job de email", ex);
        }
    }

    private TransactionalEmail fromJson(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, TransactionalEmail.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao desserializar job de email", ex);
        }
    }

    private String sanitize(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Falha no envio";
        }
        return errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
    }

    private String claimScript() {
        return """
                local pending = KEYS[1]
                local processing = KEYS[2]
                local claims = KEYS[3]
                local attempts = KEYS[4]
                local now = tonumber(ARGV[1])
                local deadline = tonumber(ARGV[2])
                local claimToken = ARGV[3]
                local jobs = redis.call('ZRANGEBYSCORE', pending, '-inf', now, 'LIMIT', 0, 1)
                if #jobs == 0 then
                    return {}
                end
                local jobId = jobs[1]
                redis.call('ZREM', pending, jobId)
                redis.call('ZADD', processing, deadline, jobId)
                redis.call('HSET', claims, jobId, claimToken)
                local attempt = redis.call('HINCRBY', attempts, jobId, 1)
                return {jobId, tostring(attempt)}
                """;
    }

    private String ackScript() {
        return """
                local processing = KEYS[1]
                local claims = KEYS[2]
                local payload = KEYS[3]
                local createdAt = KEYS[4]
                local attempts = KEYS[5]
                local errors = KEYS[6]
                local jobId = ARGV[1]
                local claimToken = ARGV[2]
                if redis.call('HGET', claims, jobId) ~= claimToken then
                    return 0
                end
                redis.call('ZREM', processing, jobId)
                redis.call('HDEL', claims, jobId)
                redis.call('HDEL', payload, jobId)
                redis.call('HDEL', createdAt, jobId)
                redis.call('HDEL', attempts, jobId)
                redis.call('HDEL', errors, jobId)
                return 1
                """;
    }

    private String retryScript() {
        return """
                local processing = KEYS[1]
                local pending = KEYS[2]
                local claims = KEYS[3]
                local jobId = ARGV[1]
                local claimToken = ARGV[2]
                local nextAt = tonumber(ARGV[3])
                if redis.call('HGET', claims, jobId) ~= claimToken then
                    return 0
                end
                redis.call('ZREM', processing, jobId)
                redis.call('HDEL', claims, jobId)
                redis.call('ZADD', pending, nextAt, jobId)
                return 1
                """;
    }

    private String deadLetterScript() {
        return """
                local processing = KEYS[1]
                local dead = KEYS[2]
                local claims = KEYS[3]
                local jobId = ARGV[1]
                local claimToken = ARGV[2]
                local now = tonumber(ARGV[3])
                if redis.call('HGET', claims, jobId) ~= claimToken then
                    return 0
                end
                redis.call('ZREM', processing, jobId)
                redis.call('HDEL', claims, jobId)
                redis.call('ZADD', dead, now, jobId)
                return 1
                """;
    }

    private String requeueStaleScript() {
        return """
                local processing = KEYS[1]
                local pending = KEYS[2]
                local claims = KEYS[3]
                local now = tonumber(ARGV[1])
                local pendingAt = tonumber(ARGV[2])
                local jobs = redis.call('ZRANGEBYSCORE', processing, '-inf', now)
                for _,jobId in ipairs(jobs) do
                    redis.call('ZREM', processing, jobId)
                    redis.call('HDEL', claims, jobId)
                    redis.call('ZADD', pending, pendingAt, jobId)
                end
                return jobs
                """;
    }

    private String idempotencyKey(String idempotencyKey) {
        return queueProperties.getKeyPrefix() + ":idempotency:" + idempotencyKey;
    }

    private String pendingKey() {
        return queueProperties.getKeyPrefix() + ":pending";
    }

    private String processingKey() {
        return queueProperties.getKeyPrefix() + ":processing";
    }

    private String deadLetterKey() {
        return queueProperties.getKeyPrefix() + ":dead";
    }

    private String payloadKey() {
        return queueProperties.getKeyPrefix() + ":payload";
    }

    private String createdAtKey() {
        return queueProperties.getKeyPrefix() + ":created-at";
    }

    private String attemptsKey() {
        return queueProperties.getKeyPrefix() + ":attempts";
    }

    private String claimKey() {
        return queueProperties.getKeyPrefix() + ":claims";
    }

    private String errorKey() {
        return queueProperties.getKeyPrefix() + ":errors";
    }
}
