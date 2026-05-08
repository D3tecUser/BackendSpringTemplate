package com.d3tec.template.nomeDoSeuProjeto.config.security;

import com.d3tec.template.nomeDoSeuProjeto.service.auth.AesGcmMfaSecretProtectionService;
import com.d3tec.template.nomeDoSeuProjeto.service.auth.MfaSecretProtectionService;
import com.d3tec.template.nomeDoSeuProjeto.service.auth.PlainTextMfaSecretProtectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

@Configuration
public class MfaSecretProtectionConfig {

    @Bean
    public MfaSecretProtectionService mfaSecretProtectionService(
            @Value("${security.mfa.secret-encryption.key:}") String encryptionKey,
            SecureRandom secureRandom
    ) {
        if (!StringUtils.hasText(encryptionKey)) {
            return new PlainTextMfaSecretProtectionService();
        }

        return new AesGcmMfaSecretProtectionService(encryptionKey, secureRandom);
    }
}
