package com.d3tec.template.nomeDoSeuProjeto.config;

import com.d3tec.template.nomeDoSeuProjeto.email.config.MailProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class EmailInfrastructureValidator {

    private final MailProperties mailProperties;
    private final Environment environment;

    @PostConstruct
    void validate() {
        if (!mailProperties.isEnabled() || !mailProperties.isFailFast()) {
            return;
        }

        require(mailProperties.getFromAddress(), "app.mail.from-address");
        require(mailProperties.getBaseUrl(), "app.mail.base-url");
        require(environment.getProperty("spring.mail.host"), "spring.mail.host");
        Integer port = environment.getProperty("spring.mail.port", Integer.class);
        if (port == null || port <= 0) {
            throw new IllegalStateException("Propriedade obrigatória ausente: spring.mail.port");
        }
    }

    private void require(String value, String property) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Propriedade obrigatória ausente: " + property);
        }
    }
}
