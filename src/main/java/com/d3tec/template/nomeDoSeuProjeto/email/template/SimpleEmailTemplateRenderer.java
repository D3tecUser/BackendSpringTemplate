package com.d3tec.template.nomeDoSeuProjeto.email.template;

import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SimpleEmailTemplateRenderer implements EmailTemplateRenderer {

    @Override
    public RenderedEmailTemplate render(EmailType emailType, Map<String, Object> model) {
        TemplateDefinition template = definitionFor(emailType);
        String htmlBody = renderTemplate(template.htmlPath(), model);
        String textBody = renderTemplate(template.textPath(), model);

        return RenderedEmailTemplate.builder()
                .subject(template.subject())
                .textBody(textBody)
                .htmlBody(htmlBody)
                .build();
    }

    private TemplateDefinition definitionFor(EmailType emailType) {
        return switch (emailType) {
            case EMAIL_VERIFICATION -> new TemplateDefinition(
                    "Confirme seu email",
                    "templates/email/verification.html",
                    "templates/email/verification.txt"
            );
            case PASSWORD_RESET -> new TemplateDefinition(
                    "Instruções de recuperação de senha",
                    "templates/email/password-reset.html",
                    "templates/email/password-reset.txt"
            );
        };
    }

    private String renderTemplate(String templatePath, Map<String, Object> model) {
        String content = loadTemplate(templatePath);
        String rendered = content;

        for (Map.Entry<String, Object> entry : model.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", asString(entry.getValue()));
        }

        return rendered;
    }

    private String loadTemplate(String templatePath) {
        ClassPathResource resource = new ClassPathResource(templatePath);
        if (!resource.exists()) {
            throw new IllegalStateException("Template de email não encontrado: " + templatePath);
        }

        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler template de email: " + templatePath, ex);
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private record TemplateDefinition(String subject, String htmlPath, String textPath) {
    }
}
