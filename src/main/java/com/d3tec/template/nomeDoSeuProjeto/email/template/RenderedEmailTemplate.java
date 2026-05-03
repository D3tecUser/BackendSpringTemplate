package com.d3tec.template.nomeDoSeuProjeto.email.template;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RenderedEmailTemplate {
    String subject;
    String htmlBody;
    String textBody;
}
