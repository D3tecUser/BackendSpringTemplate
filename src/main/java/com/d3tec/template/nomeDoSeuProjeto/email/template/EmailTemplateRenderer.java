package com.d3tec.template.nomeDoSeuProjeto.email.template;

import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailType;

import java.util.Map;

public interface EmailTemplateRenderer {
    RenderedEmailTemplate render(EmailType emailType, Map<String, Object> model);
}
