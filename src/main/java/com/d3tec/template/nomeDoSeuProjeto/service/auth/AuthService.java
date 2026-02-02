package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.dto.LoginRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.LoginResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterResponse;
import com.d3tec.template.nomeDoSeuProjeto.entity.Role;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import com.d3tec.template.nomeDoSeuProjeto.repository.RoleRepository;
import com.d3tec.template.nomeDoSeuProjeto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

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

    @Value("${spring.application.name}")
    private String issuer;

    public LoginResponse login(LoginRequest loginRequest) {
        var user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas!"));

        var roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        if ( !isLoginCorret(loginRequest.getPassword(), user.getPassword()) ) {
            throw new BadCredentialsException("Credenciais inválidas!");
        }

        var now = Instant.now();
        var expiresIn = 300L;

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("roles", roles)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .build();

        var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwtValue);

        return loginResponse;
    }

    public RegisterResponse register(RegisterRequest registerRequest) {
        // Verifica se o usuario ja existe no banco de dados
        var existingUser = userRepository.findByEmail(registerRequest.getEmail());
        if ( existingUser.isPresent() ) {
            throw new BadCredentialsException("Um usuário com esse email já existe!");
        }

        var basicRole = roleRepository.findByName(Role.RoleValues.BASIC.name())
                .orElseThrow(() -> new RuntimeException("Role não encontrada!"));

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(registerRequest.getPassword()));
        user.setRoles(Set.of(basicRole));

        var newUser = userRepository.save(user);

        return new RegisterResponse(newUser);
    }

    private boolean isLoginCorret(String loginPassword, String userPassword) {
        return bCryptPasswordEncoder.matches(loginPassword, userPassword);
    }
}
