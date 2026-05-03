package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.dto.*;
import com.d3tec.template.nomeDoSeuProjeto.email.service.ApplicationEmailService;
import com.d3tec.template.nomeDoSeuProjeto.entity.EmailTokenType;
import com.d3tec.template.nomeDoSeuProjeto.entity.Role;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import com.d3tec.template.nomeDoSeuProjeto.exception.exceptions.ConflictException;
import com.d3tec.template.nomeDoSeuProjeto.exception.exceptions.EmailNotVerifiedException;
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
    private final EmailTokenService emailTokenService;
    private final ApplicationEmailService applicationEmailService;

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

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email não verificado. Solicite um novo link de confirmação.");
        }

        bruteforceProtectionService.onLoginSuccess(keyIp);
        bruteforceProtectionService.onLoginSuccess(keyIpEmail);

        LoginResponse loginResponse = new LoginResponse();
        // MFA habilitado
        if (user.isMfaEnabled()) {
            String mfaToken = generateMfaChallengeToken(user);

            loginResponse.setAuthenticated(false);
            loginResponse.setMfaRequired(true);
            loginResponse.setEmailVerificationRequired(false);
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
        loginResponse.setEmailVerificationRequired(false);
        loginResponse.setExpiresInSeconds(expiresIn);

        return loginResponse;
    }

    public RegisterResponse register(RegisterRequest registerRequest) {
        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();
        // Verifica se o usuario ja existe no banco de dados
        var existingUser = userRepository.findByEmail(normalizedEmail);
        if ( existingUser.isPresent() ) {
            throw new ConflictException("E-mail já cadastrado");
        }

        var basicRole = roleRepository.findByName("BASIC")
                .orElseThrow(() -> new RuntimeException("Role não encontrada!"));

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(bCryptPasswordEncoder.encode(registerRequest.getPassword()));
        user.setRoles(Set.of(basicRole));
        user.setSecret(mfaTokenManager.generateSecretKey());
        user.setMfaEnabled(false);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        String rawToken = emailTokenService.create(savedUser, EmailTokenType.EMAIL_VERIFICATION, Duration.ofHours(24));
        applicationEmailService.sendEmailVerification(savedUser, rawToken);

        return RegisterResponse.builder()
                .message("Usuário cadastrado com sucesso! Verifique seu email para liberar o acesso.")
                .email(savedUser.getEmail())
                .verificationRequired(true)
                .build();
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    public GenericMessageResponse verifyEmail(String token) {
        User user = emailTokenService.consume(token, EmailTokenType.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        userRepository.save(user);
        return new GenericMessageResponse("Email verificado com sucesso!");
    }

    public GenericMessageResponse resendVerification(ResendVerificationRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    String rawToken = emailTokenService.create(user, EmailTokenType.EMAIL_VERIFICATION, Duration.ofHours(24));
                    applicationEmailService.sendEmailVerification(user, rawToken);
                });

        return new GenericMessageResponse(
                "Se o email estiver cadastrado e pendente de verificação, uma nova mensagem foi enviada."
        );
    }

    public GenericMessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(User::isEmailVerified)
                .ifPresent(user -> {
                    String rawToken = emailTokenService.create(user, EmailTokenType.PASSWORD_RESET, Duration.ofHours(2));
                    applicationEmailService.sendForgotPassword(user, rawToken);
                });

        return new GenericMessageResponse(
                "Se o email estiver cadastrado, enviaremos instruções para continuar a recuperação de senha."
        );
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
