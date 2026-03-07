package com.d3tec.template.nomeDoSeuProjeto.repository;

import com.d3tec.template.nomeDoSeuProjeto.entity.RefreshToken;
import com.d3tec.template.nomeDoSeuProjeto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteAllByUser(User user);
}
