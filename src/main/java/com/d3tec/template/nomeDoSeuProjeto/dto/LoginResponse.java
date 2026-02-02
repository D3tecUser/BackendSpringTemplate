package com.d3tec.template.nomeDoSeuProjeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginResponse {
    @Schema(example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...", description = "JWT de acesso")
    private String token;
}
