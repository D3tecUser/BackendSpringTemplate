package com.d3tec.template.nomeDoSeuProjeto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginRequest {
    @NotEmpty(message = "O email não pode estar vazio!")
    @Email(message = "Formato de email inválido")
    private String email;
    @NotEmpty(message = "A senha não deve estar vazia")
    private String password;
}
