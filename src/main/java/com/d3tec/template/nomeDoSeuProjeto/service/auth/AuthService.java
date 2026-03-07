package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.dto.*;
import com.d3tec.template.nomeDoSeuProjeto.entity.Role;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import com.d3tec.template.nomeDoSeuProjeto.exception.exceptions.ConflictException;
import com.d3tec.template.nomeDoSeuProjeto.repository.RoleRepository;
import com.d3tec.template.nomeDoSeuProjeto.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RoleRepository roleRepository;
    private final MfaTokenManager mfaTokenManager;
    private final RefreshTokenService refreshTokenService;

    private final BruteforceProtectionService bruteforceProtectionService;
    private final HttpServletRequest request;

    @Value("${jwt.token.expires.in}")
    private Long expiresIn;
    @Value("${spring.application.name}")
    private String issuer;

    public LoginResponse login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();
        String ip = clientIp(request);

        String keyIp = "ip:" + ip;
        String keyIpEmail = "ip_email:" + ip + "|" + email;

        bruteforceProtectionService.assertNotBlocked(keyIp);
        bruteforceProtectionService.assertNotBlocked(keyIpEmail);

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    bruteforceProtectionService.onLoginFailure(keyIp);
                    bruteforceProtectionService.onLoginFailure(keyIpEmail);
                    return new BadCredentialsException("Credenciais inválidas!");
                });


        var roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        if ( !passwordMatches(loginRequest.getPassword(), user.getPassword()) ) {
            bruteforceProtectionService.onLoginFailure(keyIp);
            bruteforceProtectionService.onLoginFailure(keyIpEmail);
            throw new BadCredentialsException("Credenciais inválidas!");
        }

        bruteforceProtectionService.onLoginSuccess(keyIp);
        bruteforceProtectionService.onLoginSuccess(keyIpEmail);

        LoginResponse loginResponse = new LoginResponse();
        // MFA habilitado
        if (user.isMfaEnabled()) {
            String mfaToken = generateMfaChallengeToken(user);

            loginResponse.setAuthenticated(false);
            loginResponse.setMfaRequired(true);
            loginResponse.setMfaToken(mfaToken);
            loginResponse.setExpiresInSeconds(300L);
            return loginResponse;
        }

        var now = Instant.now();

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("roles", roles)
                .claim("typ", "access")
                .claim("mfa", true)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .build();

        var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        RefreshTokenCreationDto refreshTokenDto = refreshTokenService.create(user, Duration.ofDays(7));

        loginResponse.setToken(jwtValue);
        loginResponse.setRefreshToken(refreshTokenDto.getRawToken());
        loginResponse.setAuthenticated(true);
        loginResponse.setMfaRequired(false);
        loginResponse.setExpiresInSeconds(expiresIn);

        return loginResponse;
    }

    public String register(RegisterRequest registerRequest) {
        // Verifica se o usuario ja existe no banco de dados
        var existingUser = userRepository.findByEmail(registerRequest.getEmail());
        if ( existingUser.isPresent() ) {
            throw new ConflictException("E-mail já cadastrado");
        }

        var basicRole = roleRepository.findByName("BASIC")
                .orElseThrow(() -> new RuntimeException("Role não encontrada!"));

        User user = new User();
        user.setEmail(registerRequest.getEmail().trim().toLowerCase());
        user.setPassword(bCryptPasswordEncoder.encode(registerRequest.getPassword()));
        user.setRoles(Set.of(basicRole));
        user.setSecret(mfaTokenManager.generateSecretKey());
        user.setMfaEnabled(false);

        userRepository.save(user);

        return "Usuário cadastrado com sucesso!";
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private boolean passwordMatches(String loginPassword, String userPassword) {
        return bCryptPasswordEncoder.matches(loginPassword, userPassword);
    }

    private String clientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String generateMfaChallengeToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("typ", "mfa_challenge")
                .claim("mfa", "pending")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
