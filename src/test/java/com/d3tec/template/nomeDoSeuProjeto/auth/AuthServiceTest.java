package com.d3tec.template.nomeDoSeuProjeto.auth;

import com.d3tec.template.nomeDoSeuProjeto.dto.ForgotPasswordRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.LoginRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterRequest;
import com.d3tec.template.nomeDoSeuProjeto.email.service.ApplicationEmailService;
import com.d3tec.template.nomeDoSeuProjeto.entity.EmailTokenType;
import com.d3tec.template.nomeDoSeuProjeto.entity.Role;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import com.d3tec.template.nomeDoSeuProjeto.exception.exceptions.EmailNotVerifiedException;
import com.d3tec.template.nomeDoSeuProjeto.repository.RoleRepository;
import com.d3tec.template.nomeDoSeuProjeto.repository.UserRepository;
import com.d3tec.template.nomeDoSeuProjeto.service.auth.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void registerShouldCreateUnverifiedUserAndQueueVerificationEmail() {
        JwtEncoder jwtEncoder = mock(JwtEncoder.class);
        UserRepository userRepository = mock(UserRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        RoleRepository roleRepository = mock(RoleRepository.class);
        AcessTokenService acessTokenService = mock(AcessTokenService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        EmailTokenService emailTokenService = mock(EmailTokenService.class);
        ApplicationEmailService applicationEmailService = mock(ApplicationEmailService.class);
        BruteforceProtectionService bruteforceProtectionService = mock(BruteforceProtectionService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        AuthService authService = new AuthService(
                jwtEncoder,
                userRepository,
                passwordEncoder,
                roleRepository,
                acessTokenService,
                refreshTokenService,
                emailTokenService,
                applicationEmailService,
                bruteforceProtectionService,
                request
        );

        Role basicRole = new Role();
        basicRole.setName("BASIC");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("BASIC")).thenReturn(Optional.of(basicRole));
        when(emailTokenService.create(any(User.class), any(EmailTokenType.class), any(Duration.class))).thenReturn("token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("user@example.com");
        registerRequest.setPassword("senha123");

        var response = authService.register(registerRequest);

        assertTrue(response.isVerificationRequired());
        assertEquals("user@example.com", response.getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(!userCaptor.getValue().isEmailVerified());
        verify(applicationEmailService).sendEmailVerification(userCaptor.getValue(), "token");
    }

    @Test
    void loginShouldRejectUnverifiedUsers() {
        JwtEncoder jwtEncoder = mock(JwtEncoder.class);
        UserRepository userRepository = mock(UserRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        RoleRepository roleRepository = mock(RoleRepository.class);
        AcessTokenService acessTokenService = mock(AcessTokenService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        EmailTokenService emailTokenService = mock(EmailTokenService.class);
        ApplicationEmailService applicationEmailService = mock(ApplicationEmailService.class);
        BruteforceProtectionService bruteforceProtectionService = mock(BruteforceProtectionService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        AuthService authService = new AuthService(
                jwtEncoder,
                userRepository,
                passwordEncoder,
                roleRepository,
                acessTokenService,
                refreshTokenService,
                emailTokenService,
                applicationEmailService,
                bruteforceProtectionService,
                request
        );

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setRoles(Set.of());
        user.setEmailVerified(false);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("senha123");

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(loginRequest));
        verify(bruteforceProtectionService, never()).onLoginSuccess(anyString());
    }

    @Test
    void forgotPasswordShouldBeGenericWhenUserDoesNotExist() {
        JwtEncoder jwtEncoder = mock(JwtEncoder.class);
        UserRepository userRepository = mock(UserRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        RoleRepository roleRepository = mock(RoleRepository.class);
        AcessTokenService acessTokenService = mock(AcessTokenService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        EmailTokenService emailTokenService = mock(EmailTokenService.class);
        ApplicationEmailService applicationEmailService = mock(ApplicationEmailService.class);
        BruteforceProtectionService bruteforceProtectionService = mock(BruteforceProtectionService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        AuthService authService = new AuthService(
                jwtEncoder,
                userRepository,
                passwordEncoder,
                roleRepository,
                acessTokenService,
                refreshTokenService,
                emailTokenService,
                applicationEmailService,
                bruteforceProtectionService,
                request
        );

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("missing@example.com");

        var response = authService.forgotPassword(forgotPasswordRequest);

        assertTrue(response.getMessage().contains("Se o email estiver cadastrado"));
        verify(applicationEmailService, never()).sendForgotPassword(any(User.class), anyString());
    }
}
