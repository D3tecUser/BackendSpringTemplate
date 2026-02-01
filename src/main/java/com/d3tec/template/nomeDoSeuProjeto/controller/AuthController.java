package com.d3tec.template.nomeDoSeuProjeto.controller;

import com.d3tec.template.nomeDoSeuProjeto.dto.LoginRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.LoginResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterResponse;
import com.d3tec.template.nomeDoSeuProjeto.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(
                authService.login(loginRequest)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.ok(
                authService.register(registerRequest)
        );
    }
}
