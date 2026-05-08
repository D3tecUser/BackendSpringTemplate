package com.d3tec.template.nomeDoSeuProjeto.dto.mfa;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaVerifyRequest {
    @NotBlank
    private String mfaToken;
    @NotBlank
    private String mfaCode;
}
