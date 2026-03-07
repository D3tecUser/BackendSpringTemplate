package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.dto.RefreshTokenCreationDto;
import com.d3tec.template.nomeDoSeuProjeto.dto.TokenPairDTO;
import com.d3tec.template.nomeDoSeuProjeto.entity.RefreshToken;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import com.d3tec.template.nomeDoSeuProjeto.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AcessTokenService acessTokenService;
    private final SecureRandom secureRandom;

    public RefreshTokenCreationDto create(User user, Duration duration) {
        String rawToken = generateSecureToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plus(duration));
        refreshToken.setExpired(false);

        refreshTokenRepository.save(refreshToken);

        return new RefreshTokenCreationDto(rawToken, refreshToken.getExpiresAt());
    }

    public User validateAndGetUser(String rawToken) {
        String hash = hashToken(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));

        if (token.getExpired()) {
            throw new BadCredentialsException("Refresh token revogado");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expirado");
        }

        return token.getUser();
    }

    public void revoke(String rawToken) {
        String hash = hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setExpired(true);
            token.setExpiredAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public TokenPairDTO refresh(String refreshToken) {
        User user = validateAndGetUser(refreshToken);

        revoke(refreshToken);
        var newRefreshToken = create(user, Duration.ofDays(7));

        // emite JWT final
        String newJwt = acessTokenService.getAcessToken(user);
        return new TokenPairDTO(newJwt, newRefreshToken.getRawToken());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        return DigestUtils.sha256Hex(rawToken);
    }
}
