package com.d3tec.template.nomeDoSeuProjeto.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class ApiErrorResponse {
    private HttpStatus status;
    private String error;
    private String message;
    private String path;
    private Instant timestamp;
    List<FieldViolation> fieldViolations;
}
