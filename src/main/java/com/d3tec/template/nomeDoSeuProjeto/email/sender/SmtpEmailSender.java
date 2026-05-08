package com.d3tec.template.nomeDoSeuProjeto.email.sender;

import com.d3tec.template.nomeDoSeuProjeto.email.config.MailProperties;
import com.d3tec.template.nomeDoSeuProjeto.email.model.TransactionalEmail;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void send(TransactionalEmail email) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(email.getTo());
            helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
            helper.setSubject(email.getSubject());
            helper.setText(email.getTextBody(), email.getHtmlBody());
            javaMailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao enviar email SMTP", ex);
        }
    }
}
