package com.d3tec.template.nomeDoSeuProjeto.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(
            sequenceName = "users_seq_generator",
            name = "users_seq_generator",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "users_seq_generator"
    )
    private Long id;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(length = 255, nullable = false)
    @JsonIgnore
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @JsonIgnore
    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;
    @JsonIgnore
    @Column(name = "secret", length = 64)
    private String secret;
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    private LocalDateTime createdAt =  LocalDateTime.now();
    private LocalDateTime updatedAt;
}
