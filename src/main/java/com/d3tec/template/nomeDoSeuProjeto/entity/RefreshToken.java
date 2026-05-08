package com.d3tec.template.nomeDoSeuProjeto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @SequenceGenerator(
        sequenceName = "refresh_token_seq_generator",
        name = "refresh_token_seq_generator",
        allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "refresh_token_seq_generator"
    )
    private Long id;
    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Column(nullable = false)
    private Boolean expired = false;

    @Column(nullable = false)
    private Boolean mfaVerified = false;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;
    private Instant expiredAt;
}
