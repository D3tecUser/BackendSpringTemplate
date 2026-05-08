package com.d3tec.template.nomeDoSeuProjeto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "O email não pode estar vazio!")
    @Email(message = "Formato de email inválido")
    private String email;
}
