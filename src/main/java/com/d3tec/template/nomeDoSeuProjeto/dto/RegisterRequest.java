package com.d3tec.template.nomeDoSeuProjeto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email
    private String email;
    @Size(min = 6, max = 64)
    private String password;

    // TODO -> COLOQUE AQUI OS OUTROS ATRIBUTOS DO REGISTRO
}
