package com.d3tec.template.nomeDoSeuProjeto.email.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {
    private boolean enabled = true;
    private String fromName;
    private String fromAddress;
    private String baseUrl;
    private boolean failFast = true;
    private String transport = "smtp";
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {
        private long minIntervalMs = 1000;
    }
}
