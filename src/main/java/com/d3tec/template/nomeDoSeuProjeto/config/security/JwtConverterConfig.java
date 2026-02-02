package com.d3tec.template.nomeDoSeuProjeto.config.security;

import com.d3tec.template.nomeDoSeuProjeto.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@Configuration
public class JwtConverterConfig {

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(
            UserRepository userRepository
    ) {
        return jwt -> {
            Long userId = Long.valueOf(jwt.getSubject());

            var roles = jwt.getClaimAsStringList("roles");
            if (roles == null) roles = List.of();

            var authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();

            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

            var principal = new UsuarioPrincipal(user, authorities);

            return new UsernamePasswordAuthenticationToken(
                    principal,
                    jwt.getTokenValue(),
                    authorities
            );
        };
    }
}
