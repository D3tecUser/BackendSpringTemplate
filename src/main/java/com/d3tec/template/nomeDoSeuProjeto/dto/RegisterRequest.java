package com.d3tec.template.nomeDoSeuProjeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email
    @Schema(example = "usuario@usuario.com", description = "E-mail do usuário", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    @Size(min = 6, max = 64)
    @Schema(example = "senha123", description = "Senha do usuário", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    // TODO -> COLOQUE AQUI OS OUTROS ATRIBUTOS DO REGISTRO
}
