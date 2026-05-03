package com.d3tec.template.nomeDoSeuProjeto.repository;

import com.d3tec.template.nomeDoSeuProjeto.entity.EmailToken;
import com.d3tec.template.nomeDoSeuProjeto.entity.EmailTokenType;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {
    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type);
    void deleteAllByUserAndType(User user, EmailTokenType type);
}
