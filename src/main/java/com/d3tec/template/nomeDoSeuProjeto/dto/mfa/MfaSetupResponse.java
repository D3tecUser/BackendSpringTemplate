package com.d3tec.template.nomeDoSeuProjeto.dto.mfa;

import lombok.Data;

@Data
public class MfaSetupResponse {
    private boolean mfaEnabled;
    private String qrCodeDataUri;
}
