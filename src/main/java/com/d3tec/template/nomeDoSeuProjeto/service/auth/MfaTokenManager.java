package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class MfaTokenManager {
    private static final Base32 BASE32 = new Base32();
    private static final SecureRandom RANDOM = new SecureRandom();

    // Padrões TOTP
    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_PERIOD_SECONDS = 30;
    private static final int DEFAULT_ALLOWED_SKEW_STEPS = 1; // aceita +/- 30s

    // Tamanho do QR
    private static final int QR_SIZE = 240;

    @Value("${spring.application.name}")
    private String issuer;

    // Gera um secret em Base32 compatível com otpauth.
    public String generateSecretKey() {
        byte[] raw = new byte[20]; // 160 bits
        RANDOM.nextBytes(raw);
        return BASE32.encodeToString(raw).replace("=", "");
    }

    public String generateQrCode(String accountEmail, String base32Secret) {
        String otpAuth = buildOtpAuthUri(issuer, accountEmail, base32Secret,
                DEFAULT_DIGITS, DEFAULT_PERIOD_SECONDS, "SHA1");

        try {
            var matrix = new QRCodeWriter().encode(otpAuth, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            String b64 = Base64.getEncoder().encodeToString(out.toByteArray());
            return "data:image/png;base64," + b64;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar QR Code", e);
        }
    }

    // Verifica o código TOTP (6 dígitos) com tolerância de tempo.
    public boolean verifyTotp(String code, String base32Secret) {
        if (code == null || base32Secret == null) return false;

        String normalized = code.replaceAll("\\s+", "");
        if (!normalized.matches("\\d{6}")) return false;

        try {
            byte[] secretBytes = BASE32.decode(base32Secret);
            var key = new SecretKeySpec(secretBytes, "HmacSHA1");

            var totp = new TimeBasedOneTimePasswordGenerator(
                    Duration.ofSeconds(DEFAULT_PERIOD_SECONDS),
                    DEFAULT_DIGITS
            );

            Instant now = Instant.now();

            for (int step = -DEFAULT_ALLOWED_SKEW_STEPS; step <= DEFAULT_ALLOWED_SKEW_STEPS; step++) {
                Instant t = now.plusSeconds((long) step * DEFAULT_PERIOD_SECONDS);
                int otp = totp.generateOneTimePassword(key, t);
                String expected = String.format("%06d", otp);

                if (expected.equals(normalized)) return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // Monta o otpauth URI padrão (compatível com Google Authenticator).
    private static String buildOtpAuthUri(
            String issuer,
            String account,
            String secretBase32,
            int digits,
            int periodSeconds,
            String algorithm
    ) {
        String label = urlEncode(issuer + ":" + account);

        return "otpauth://totp/" + label +
                "?secret=" + urlEncode(secretBase32) +
                "&issuer=" + urlEncode(issuer) +
                "&digits=" + digits +
                "&period=" + periodSeconds +
                "&algorithm=" + urlEncode(algorithm);
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
