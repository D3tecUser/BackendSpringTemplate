package com.d3tec.template.nomeDoSeuProjeto.dto.mfa;

import lombok.Data;

@Data
public class MfaVerifyRequest {
    private String mfaToken;
    private String mfaCode;
}
