package com.d3tec.template.nomeDoSeuProjeto.email.sender;

import com.d3tec.template.nomeDoSeuProjeto.email.model.TransactionalEmail;

public interface EmailSender {
    void send(TransactionalEmail email);
}
