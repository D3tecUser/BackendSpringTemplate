package com.d3tec.template.nomeDoSeuProjeto.email.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionalEmail {
    String to;
    String subject;
    String htmlBody;
    String textBody;
    String idempotencyKey;
    EmailType type;
}
