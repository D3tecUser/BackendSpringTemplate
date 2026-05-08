package com.d3tec.template.nomeDoSeuProjeto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_token")
@Getter
@Setter
@NoArgsConstructor
public class EmailToken {

    @Id
    @SequenceGenerator(
            sequenceName = "email_token_seq_generator",
            name = "email_token_seq_generator",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_token_seq_generator"
    )
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EmailTokenType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant consumedAt;
}
