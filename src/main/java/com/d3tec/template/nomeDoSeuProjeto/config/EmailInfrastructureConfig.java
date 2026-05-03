package com.d3tec.template.nomeDoSeuProjeto.config;

import com.d3tec.template.nomeDoSeuProjeto.email.config.EmailQueueProperties;
import com.d3tec.template.nomeDoSeuProjeto.email.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
@EnableConfigurationProperties({MailProperties.class, EmailQueueProperties.class})
public class EmailInfrastructureConfig {

    private final EmailQueueProperties queueProperties;

    @Bean(name = "emailQueueExecutor")
    public TaskExecutor emailQueueExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("email-queue-");
        executor.setCorePoolSize(Math.max(1, queueProperties.getWorkerConcurrency()));
        executor.setMaxPoolSize(Math.max(1, queueProperties.getWorkerConcurrency()));
        executor.setQueueCapacity(Math.max(10, queueProperties.getBatchSize() * 4));
        executor.initialize();
        return executor;
    }
}
