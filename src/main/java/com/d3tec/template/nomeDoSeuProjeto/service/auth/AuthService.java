package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.dto.LoginRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.LoginResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.mfa.MfaSetupResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.mfa.MfaVerifyRequest;
import com.d3tec.template.nomeDoSeuProjeto.entity.Role;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import com.d3tec.template.nomeDoSeuProjeto.exception.exceptions.ApiException;
import com.d3tec.template.nomeDoSeuProjeto.exception.exceptions.ConflictException;
import com.d3tec.template.nomeDoSeuProjeto.repository.RoleRepository;
import com.d3tec.template.nomeDoSeuProjeto.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RoleRepository roleRepository;
    private final MfaTokenManager mfaTokenManager;

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

        if ( !isLoginCorret(loginRequest.getPassword(), user.getPassword()) ) {
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
            loginResponse.setExpiresInSeconds(3600L);
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


        loginResponse.setToken(jwtValue);
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
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token expirado");
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
        var roles = user.getRoles().stream().map(Role::getName).toList();
        Instant now = Instant.now();

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("roles", roles)
                .claim("typ", "access")
                .claim("mfa", true)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .build();

        String jwtFinal = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        LoginResponse resp = new LoginResponse();
        resp.setAuthenticated(true);
        resp.setMfaRequired(false);
        resp.setToken(jwtFinal);
        resp.setExpiresInSeconds(expiresIn);
        return resp;
    }

    private boolean isLoginCorret(String loginPassword, String userPassword) {
        return bCryptPasswordEncoder.matches(loginPassword, userPassword);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
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
