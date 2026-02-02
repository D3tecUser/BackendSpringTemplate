package com.d3tec.template.nomeDoSeuProjeto.exception.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException{
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
