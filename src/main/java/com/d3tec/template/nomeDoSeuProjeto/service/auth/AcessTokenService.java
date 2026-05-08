package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.entity.Role;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AcessTokenService {

    private final JwtEncoder jwtEncoder;
    @Value("${spring.application.name}")
    private String issuer;
    @Value("${jwt.token.expires.in}")
    private Long expiresIn;

    public String getAcessToken(User user, boolean mfaVerified) {
        var roles = user.getRoles().stream().map(Role::getName).toList();
        var privileges = user.getRoles().stream()
                .filter(role -> role.getPrivileges() != null)
                .flatMap(role -> role.getPrivileges().stream())
                .map(privilege -> privilege.getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Instant now = Instant.now();

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("roles", roles)
                .claim("privileges", privileges.stream().toList())
                .claim("typ", "access")
                .claim("mfa_verified", mfaVerified)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
