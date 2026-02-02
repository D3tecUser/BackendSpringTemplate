package com.d3tec.template.nomeDoSeuProjeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginRequest {
    @NotEmpty(message = "O email não pode estar vazio!")
    @Email(message = "Formato de email inválido")
    @Schema(example = "admin@admin.com", description = "E-mail do usuário")
    private String email;
    @NotEmpty(message = "A senha não deve estar vazia")
    @Schema(example = "Admin@123", description = "Senha do usuário")
    private String password;
}
