package com.d3tec.template.nomeDoSeuProjeto.controller.auth;

import com.d3tec.template.nomeDoSeuProjeto.dto.RefreshRequest;
import com.d3tec.template.nomeDoSeuProjeto.dto.mfa.MfaSetupResponse;
import com.d3tec.template.nomeDoSeuProjeto.service.auth.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/refresh")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping
    @Operation(
            summary = "Gera novamente um refresh token e um Acess Token",
            description = "Renova a autenticação"
    )
    @ApiResponses(value = {@ApiResponse(
            responseCode = "200",
            description = "Um novo jwt foi gerado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RefreshRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Par de tokens",
                                    value = """
                                {
                                  "acessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                  "refreshToken": "99b5fec941ae8fd37e."
                                }
                                """
                            )
                    }
            )
    )})
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(
                refreshTokenService.refresh(request.getRefreshToken())
        );
    }
}
