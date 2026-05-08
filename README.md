# D3TEC - DEVELOPERS TEMPUS COMPUTATRUM
# Template Backend Spring Boot (JWT + MFA)

Este template foi idealizado e mantido por **Miguel Silvano** ([github.com/MiguelSGJ](https://github.com/MiguelSGJ)),
com o objetivo de oferecer uma base sólida, moderna e reutilizável para novos projetos backend.

## Resumo
Este projeto é um template de API backend em Java com Spring Boot pensado para agilizar o desenvolvimento de software, reduzindo o tempo gasto com configuração inicial e entregando, desde o início, uma estrutura pronta para autenticação, segurança e evolução do sistema.

Ele já inclui:
- autenticação com JWT (access token)
- refresh token com endpoint de renovação
- MFA (TOTP) com setup por QR Code e validação
- controle de tentativas de login (proteção contra brute force)
- documentação OpenAPI/Swagger no perfil de desenvolvimento
- persistência com PostgreSQL + migrações Flyway

## Versões principais
- Java: **21**
- Spring Boot: **4.0.1**

## Pré-requisitos
- Java 21 instalado
- Maven (ou usar o wrapper `./mvnw` já no projeto)
- PostgreSQL em execução

## Como usar este projeto como template no GitHub

### 1. Criar um novo repositório a partir do template
1. Acesse a página do repositório no GitHub.
2. Clique em **Use this template**.
3. Escolha **Create a new repository**.
4. Defina nome, visibilidade e organização do novo projeto (** https://github.com/D3TECej **).
5. Clique em **Create repository**.

### 2. Clonar o seu novo repositório
```bash
git clone <url-do-seu-novo-repositorio>
cd <nome-do-seu-repositorio>
```

### 3. Ajustar identidade e configurações do projeto
Recomendado ajustar antes de iniciar as features:
1. Renomear `artifactId`, `name` e `description` no `pom.xml`.
2. Renomear o package base `com.d3tec.template.nomeDoSeuProjeto` para o package do seu domínio.
3. Alterar `spring.application.name` nos arquivos de properties.
4. Trocar as chaves JWT (`src/main/resources/app.key` e `src/main/resources/app.pub`) por chaves próprias.
5. Revisar as migrações em `src/main/resources/migrations`.

### 4. Configurar ambiente local
Edite os arquivos:
- `src/main/resources/application-dev.properties`
- `src/main/resources/application.properties`

Campos mais importantes:
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `bootstrap.admin.email`
- `bootstrap.admin.password`

### 5. Gerar chaves JWT (privada e pública)
Na raiz do projeto, execute:
```bash
openssl genrsa -out src/main/resources/app.key 2048
openssl rsa -in src/main/resources/app.key -pubout -out src/main/resources/app.pub
```

Esses arquivos são usados pelas propriedades:
- `jwt.private.key=classpath:app.key`
- `jwt.public.key=classpath:app.pub`

### 6. Executar em desenvolvimento
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 7. Validar que a API está funcionando
Com a aplicação rodando no perfil `dev`:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Fluxo inicial recomendado para teste de autenticação:
1. `POST /auth/register`
2. `POST /auth/login`
3. `POST /mfa/verify` (se MFA estiver ativo)
4. `POST /refresh`
5. `GET /auth/logout/{token}`

## Estrutura rápida
- `src/main/java/.../controller/auth`: endpoints de autenticação, refresh e MFA
- `src/main/java/.../service/auth`: regras de login, tokens, MFA e segurança
- `src/main/resources/migrations`: scripts Flyway
- `src/main/resources/application-*.properties`: configuração por ambiente

## Testes
Os testes de integração usam **Testcontainers** com PostgreSQL e perfil `test`.

Para executar localmente:
```bash
./mvnw test
```

Pré-requisito importante:
- Docker em execução (o Testcontainers precisa disso para subir o banco de teste).

No CI (GitHub Actions), use runner Linux com Docker disponível (ex.: `ubuntu-latest`) e execute `./mvnw test`.

## Comandos úteis
```bash
# Rodar testes
./mvnw test

# Gerar pacote
./mvnw clean package
```

## Diretrizes GitHub (Resumo)
Arquivo: docs/GITHUB_PROJECT_GUIDELINES_SUMARIO.md

