package com.d3tec.template.nomeDoSeuProjeto.dto;

import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import lombok.Data;

@Data
public class RegisterResponse {
    private String email;

    public RegisterResponse(User user) {
        this.email = user.getEmail();
    }
}
