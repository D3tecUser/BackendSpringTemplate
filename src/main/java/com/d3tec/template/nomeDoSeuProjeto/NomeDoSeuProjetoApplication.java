package com.d3tec.template.nomeDoSeuProjeto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class NomeDoSeuProjetoApplication {

	public static void main(String[] args) {
		SpringApplication.run(NomeDoSeuProjetoApplication.class, args);
	}

}
