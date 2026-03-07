package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.dto.LoginResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.RefreshTokenCreationDto;
import com.d3tec.template.nomeDoSeuProjeto.dto.mfa.MfaSetupResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.mfa.MfaVerifyRequest;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import com.d3tec.template.nomeDoSeuProjeto.exception.exceptions.ApiException;
import com.d3tec.template.nomeDoSeuProjeto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final JwtDecoder jwtDecoder;
    private final MfaTokenManager mfaTokenManager;
    private final UserRepository userRepository;
    private final AcessTokenService acessTokenService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.token.expires.in}")
    private Long expiresIn;

    public MfaSetupResponse mfaSetupForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        if (user.isMfaEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Usuário ja possui o mfa habilitado!");
        }

        // se não tem secret por algum motivo, gere
        if (user.getSecret() == null || user.getSecret().isBlank()) {
            user.setSecret(mfaTokenManager.generateSecretKey());
            userRepository.save(user);
        }

        MfaSetupResponse resp = new MfaSetupResponse();
        resp.setMfaEnabled(user.isMfaEnabled());
        resp.setQrCodeDataUri(mfaTokenManager.generateQrCode(user.getEmail(), user.getSecret()));

        return resp;
    }

    public void confirmMfa(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        if (user.isMfaEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Usuário ja possui o mfa habilitado!");
        }

        if (user.getSecret() == null || user.getSecret().isBlank()) {
            throw new BadCredentialsException("MFA não iniciado");
        }

        if (!mfaTokenManager.verifyTotp(code, user.getSecret())) {
            throw new BadCredentialsException("Código MFA inválido!");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public LoginResponse verifyMfa(MfaVerifyRequest req) {
        Jwt jwt = null;
        try{
            jwt = jwtDecoder.decode(req.getMfaToken());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token MFA inválido ou expirado");
        }

        // valida claims
        if (!"mfa_challenge".equals(jwt.getClaimAsString("typ")) ||
                !"pending".equals(jwt.getClaimAsString("mfa"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token MFA inválido!");
        }

        Long userId = Long.valueOf(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Token MFA inválido!"));

        if (!user.isMfaEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MFA não habilitado!");
        }

        if (!mfaTokenManager.verifyTotp(req.getMfaCode(), user.getSecret())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Código MFA inválido!");
        }

        // emite JWT final
        var jwtFinal = acessTokenService.getAcessToken(user);
        RefreshTokenCreationDto refreshTokenDto = refreshTokenService.create(user, Duration.ofDays(7));

        LoginResponse resp = new LoginResponse();
        resp.setAuthenticated(true);
        resp.setMfaRequired(false);
        resp.setToken(jwtFinal);
        resp.setRefreshToken(refreshTokenDto.getRawToken());
        resp.setExpiresInSeconds(expiresIn);
        return resp;
    }
}
