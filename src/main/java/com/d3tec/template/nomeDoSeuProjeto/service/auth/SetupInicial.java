package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.entity.Role;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import com.d3tec.template.nomeDoSeuProjeto.repository.RoleRepository;
import com.d3tec.template.nomeDoSeuProjeto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SetupInicial implements ApplicationRunner {
    private final BCryptPasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final Environment environment;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerPath;

    @Value("${bootstrap.admin.email}")
    private String adminEmail;

    @Value("${bootstrap.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Optional<User> adminExists = userRepository.findByRole("ADMIN");
        if (adminExists.isPresent()) {
            return;
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.warn("Setup inicial: email já existe, mas nenhum admin foi encontrado nas roles. Verifique dados.");
            return;
        }

        var adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ADMIN não encontrada!"));

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRoles(Set.of(adminRole));

        userRepository.save(admin);

        log.info("Setup inicial: usuário ADMIN criado com email={}", adminEmail);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSwaggerUrl() {
        if (!environment.acceptsProfiles(Profiles.of("dev")))
            return;

        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));

        String base = "http://localhost:" + port + (contextPath == null ? "" : contextPath);

        String url = swaggerPath.startsWith("/") ? base + swaggerPath : base + "/" + swaggerPath;

        log.info("Swagger UI disponível em: {}", url);

        log.info("OpenAPI JSON disponível em: {}/v3/api-docs", base);
    }
}
