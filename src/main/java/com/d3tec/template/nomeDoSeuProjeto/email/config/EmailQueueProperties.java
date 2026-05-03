package com.d3tec.template.nomeDoSeuProjeto.email.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.email.queue")
public class EmailQueueProperties {
    private boolean enabled = true;
    private boolean workerEnabled = true;
    private String keyPrefix = "email";
    private int claimTtlSeconds = 120;
    private int maxAttempts = 5;
    private int batchSize = 10;
    private long pollDelayMs = 2000;
    private long staleJobRequeueDelayMs = 30000;
    private long initialBackoffSeconds = 30;
    private long maxBackoffSeconds = 900;
    private long idempotencyTtlHours = 24;
    private int workerConcurrency = 1;
}
