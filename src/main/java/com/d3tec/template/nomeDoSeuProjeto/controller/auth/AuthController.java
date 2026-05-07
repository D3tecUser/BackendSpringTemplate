package com.d3tec.template.nomeDoSeuProjeto.controller.auth;

import com.d3tec.template.nomeDoSeuProjeto.config.security.UsuarioPrincipal;
import com.d3tec.template.nomeDoSeuProjeto.dto.LoginRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.LoginResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.ForgotPasswordRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.GenericMessageResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.RefreshRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.RegisterResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.ResendVerificationRequest;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.ok(
                authService.register(registerRequest)
        );
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Confirmar email", description = "Confirma o email do usuário a partir de um token enviado por email.")
    @SecurityRequirement(name = "")
    public ResponseEntity<GenericMessageResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar email de confirmação", description = "Reenvia o email de confirmação para usuários ainda não verificados.")
    @SecurityRequirement(name = "")
    public ResponseEntity<GenericMessageResponse> resendVerification(
            @RequestBody @Valid ResendVerificationRequest request
    ) {
        return ResponseEntity.ok(authService.resendVerification(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperação de senha", description = "Envia instruções por email para continuar o fluxo de recuperação.")
    @SecurityRequirement(name = "")
    public ResponseEntity<GenericMessageResponse> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Deslogue do sistema.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(@ApiResponse(responseCode = "204", description = "No content"))
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal,
            @RequestBody @Valid RefreshRequest request
    ) {
        authService.logout(usuarioPrincipal.getUserDto().getId(), request);
        return ResponseEntity.noContent().build();
    }
}
