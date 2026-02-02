package com.d3tec.template.nomeDoSeuProjeto.controller;

import com.d3tec.template.nomeDoSeuProjeto.dto.LoginRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.LoginResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterRequest;
import com.d3tec.template.nomeDoSeuProjeto.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Autenticação", description = "Endpoints de autenticação e cadastro")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido",
                    content = @Content)
    })
    @SecurityRequirement(name = "")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(
                authService.login(loginRequest)
        );
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastro", description = "Cadastra um usuário com role básica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário cadastrado com sucesso",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Role não encontrada",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido",
                    content = @Content)
    })
    @SecurityRequirement(name = "")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.ok(
                authService.register(registerRequest)
        );
    }
}
