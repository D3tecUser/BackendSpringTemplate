package com.d3tec.template.nomeDoSeuProjeto.controller.auth;

import com.d3tec.template.nomeDoSeuProjeto.config.security.UsuarioPrincipal;
import com.d3tec.template.nomeDoSeuProjeto.dto.LoginResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.mfa.MfaConfirmRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.mfa.MfaSetupResponse;
import com.d3tec.template.nomeDoSeuProjeto.dto.mfa.MfaVerifyRequest;
import com.d3tec.template.nomeDoSeuProjeto.service.auth.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mfa")
@RequiredArgsConstructor
@Tag(name = "MFA", description = "Endpoints para setup e validação de autenticação em dois fatores (TOTP).")
public class MfaController {

    private final MfaService mfaService;

    @GetMapping("/setup")
    @Operation(
            summary = "Gerar dados de setup do MFA (QR Code)",
            description = """
                    Retorna os dados necessários para configurar MFA (TOTP) em um aplicativo autenticador \
                    (Google Authenticator, Microsoft Authenticator etc).

                    Requer JWT de acesso no header Authorization (Bearer).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dados do setup retornados com sucesso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MfaSetupResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "MFA ainda desabilitado (retorna QR)",
                                            value = """
                                {
                                  "mfaEnabled": false,
                                  "qrCodeDataUri": "data:image/png;base64,iVBORw0KGgoAAA..."
                                }
                                """
                                    ),
                                    @ExampleObject(
                                            name = "MFA já habilitado (sem QR)",
                                            value = """
                                {
                                  "mfaEnabled": true
                                }
                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado (JWT ausente/inválido)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão de acesso",
                    content = @Content
            )
    })
    public ResponseEntity<?> mfaSetup(@AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal) {
        return ResponseEntity.ok(
                mfaService.mfaSetupForUser(
                        usuarioPrincipal.getUserDto().getId()
                ));
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "Confirmar e habilitar MFA (valida o primeiro código TOTP)",
            description = """
                    Após escanear o QR Code no aplicativo autenticador, o usuário envia o código TOTP (6 dígitos).
                    Se o código estiver correto, o MFA é habilitado para a conta.

                    Requer JWT de acesso no header Authorization (Bearer).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "MFA habilitado com sucesso (sem conteúdo)"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request inválido (validação de body)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso", content = @Content),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito/estado inválido (ex.: MFA não iniciado/secret ausente)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Código TOTP gerado pelo aplicativo autenticador (6 dígitos).",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MfaConfirmRequest.class),
                    examples = @ExampleObject(value = """
                    { "code": "123456" }
                    """)
            )
    )
    public ResponseEntity<?> confirmMfa(@AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal,
                                        @Valid @RequestBody MfaConfirmRequest req) {
        mfaService.confirmMfa(
                usuarioPrincipal.getUserDto().getId(),
                req.getCode()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verificar MFA no login (troca challenge token por JWT final)",
            description = """
                    Usado quando /auth/login retorna mfaRequired=true.
                    Envie:
                    - mfaToken: token de desafio retornado no login (curta duração)
                    - code: código TOTP (6 dígitos)

                    Se válido, retorna o JWT final de acesso.

                    Requer JWT de acesso no header Authorization (Bearer), conforme seu controller atual.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "MFA verificado e JWT final emitido com sucesso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = """
                        {
                          "authenticated": true,
                          "mfaRequired": false,
                          "token": "eyJhbGciOiJSUzI1NiJ9.eyJ0eXAiOiJhY2Nlc3MiLCJtZmEiOnRydWUsInJvbGVzIjpbIkFETUlOIl0sImlzcyI6IkFnb3JhQXBpIiwic3ViIjoiMSIsImlhdCI6MTc3MTQzOTg0NiwgImV4cCI6MTc3MTQ4MzA0Nn0....",
                          "expiresInSeconds": 3600
                        }
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request inválido (validação de body)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso", content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Token de desafio MFA e código TOTP (6 dígitos).",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MfaVerifyRequest.class),
                    examples = @ExampleObject(value = """
                    {
                      "mfaToken": "eyJhbGciOiJSUzI1NiJ9.eyJ0eXAiOiJtZmFfY2hhbGxlbmdlIiwibWZhIjoicGVuZGluZyIsInN1YiI6IjEiLCJleHAiOjE3NzE0...",
                      "code": "123456"
                    }
                    """)
            )
    )
    public ResponseEntity<?> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest req) {
        return ResponseEntity.ok(
                mfaService.verifyMfa(
                        req
                )
        );
    }
}
