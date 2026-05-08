package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlainTextMfaSecretProtectionService implements MfaSecretProtectionService {

    public PlainTextMfaSecretProtectionService() {
        log.warn("Chave de criptografia de segredo MFA nao encontrada. Segredo MFA será salvo sem criptografia.");
    }

    @Override
    public String protect(String rawSecret) {
        return rawSecret;
    }

    @Override
    public String reveal(String storedSecret) {
        return storedSecret;
    }

    @Override
    public boolean isProtectionEnabled() {
        return false;
    }
}
