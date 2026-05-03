package com.d3tec.template.nomeDoSeuProjeto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "privilege")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Privilege implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(
            sequenceName = "privilege_seq_generator",
            name = "privilege_seq_generator",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "privilege_seq_generator"
    )
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private LocalDateTime createdAt;
}
