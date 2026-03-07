package com.d3tec.template.nomeDoSeuProjeto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenPairDTO {
    private String acessToken;
    private String refreshToken;
}
