package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AesGcmMfaSecretProtectionService implements MfaSecretProtectionService {

    private static final String ENCRYPTED_PREFIX = "enc:";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AesGcmMfaSecretProtectionService(String rawKey, SecureRandom secureRandom) {
        this.secretKey = new SecretKeySpec(decodeKey(rawKey), "AES");
        this.secureRandom = secureRandom;
    }

    @Override
    public String protect(String rawSecret) {
        if (!StringUtils.hasText(rawSecret)) {
            return rawSecret;
        }

        if (rawSecret.startsWith(ENCRYPTED_PREFIX)) {
            return rawSecret;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(rawSecret.getBytes(StandardCharsets.UTF_8));

            ByteBuffer payload = ByteBuffer.allocate(iv.length + encrypted.length);
            payload.put(iv);
            payload.put(encrypted);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao proteger o segredo do MFA.", ex);
        }
    }

    @Override
    public String reveal(String storedSecret) {
        if (!StringUtils.hasText(storedSecret)) {
            return storedSecret;
        }

        if (!storedSecret.startsWith(ENCRYPTED_PREFIX)) {
            return storedSecret;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(storedSecret.substring(ENCRYPTED_PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(payload);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao ler o segredo do MFA.", ex);
        }
    }

    @Override
    public boolean isProtectionEnabled() {
        return true;
    }

    @Override
    public boolean requiresMigration(String storedSecret) {
        return StringUtils.hasText(storedSecret) && !storedSecret.startsWith(ENCRYPTED_PREFIX);
    }

    private static byte[] decodeKey(String rawKey) {
        try {
            byte[] key = Base64.getDecoder().decode(rawKey);
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw new IllegalArgumentException("Invalid AES key length");
            }
            return key;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("A chave de criptografia do MFA está inválida. Use uma chave AES em Base64.", ex);
        }
    }
}
