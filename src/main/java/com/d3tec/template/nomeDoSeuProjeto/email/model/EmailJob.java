package com.d3tec.template.nomeDoSeuProjeto.email.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class EmailJob {
    String id;
    TransactionalEmail email;
    int attempt;
    Instant createdAt;
    EmailJobStatus status;
}
