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
- sistema de email transacional com abstração de fila e implementação padrão em Redis
- documentação OpenAPI/Swagger no perfil de desenvolvimento
- persistência com PostgreSQL + migrações Flyway

## Versões principais
- Java: **21**
- Spring Boot: **4.0.1**

## Pré-requisitos
- Java 21 instalado
- Maven (ou usar o wrapper `./mvnw` já no projeto)
- PostgreSQL em execução
- Redis em execução
- SMTP disponível (MailHog/Mailpit local ou provedor real)

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
- `spring.data.redis.host`
- `spring.data.redis.port`
- `spring.mail.host`
- `spring.mail.port`
- `app.mail.from-address`
- `app.mail.base-url`
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
2. abrir o link enviado por email em `GET /auth/verify-email?token=...`
3. `POST /auth/login`
4. `POST /mfa/verify` (se MFA estiver ativo)
5. `POST /refresh`
6. `GET /auth/logout/{token}`

Fluxos novos de email:
1. `POST /auth/register` cria usuário não verificado e publica email na fila Redis
2. `POST /auth/resend-verification` reenfileira o email de confirmação
3. `POST /auth/forgot-password` envia instruções para continuidade do fluxo de recuperação

## Estrutura rápida
- `src/main/java/.../controller/auth`: endpoints de autenticação, refresh e MFA
- `src/main/java/.../service/auth`: regras de login, tokens, MFA e segurança
- `src/main/java/.../email`: fila abstrata, adapter Redis, templates e envio SMTP
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
### Como criar novos emails sem conhecer a implementação
O ponto de entrada para features é o serviço `ApplicationEmailService`.

Para um novo caso de uso, o desenvolvedor precisa apenas:
1. adicionar o novo tipo em `EmailType`
2. adicionar o template correspondente no renderer
3. criar um método descritivo em `ApplicationEmailService`
4. chamar esse método a partir do serviço da feature

Exemplo para postagem criada:
```java
applicationEmailService.sendPostCreated(
    PostCreatedEmailPayload.builder()
        .recipient(usuario.getEmail())
        .postId(post.getId())
        .postTitle(post.getTitle())
        .build()
);
```

## Comandos úteis
```bash
# Rodar testes
./mvnw test

# Gerar pacote
./mvnw clean package
```

## Diretrizes GitHub (Resumo)
Arquivo: docs/GITHUB_PROJECT_GUIDELINES_SUMARIO.md

## Deploy do sistema de email
Checklist mínimo:
- Redis acessível pela aplicação
- SMTP configurado em `spring.mail.*`
- URL pública correta em `app.mail.base-url`
- domínio remetente autenticado com SPF, DKIM e DMARC
- caixa remetente consistente (`app.mail.from-address`)

Observações operacionais:
- O Redis é usado apenas para enfileiramento e retry dos jobs de email.
- O envio real continua sendo feito por SMTP.
- A abstração `EmailQueuePort` permite adicionar outra implementação de fila no futuro sem alterar os fluxos de autenticação.

## Docker (deploy e teste)

### Subir ambiente de teste local (com Mailpit)
```bash
cp .env.test.example .env.test
docker compose --env-file .env.test -f docker-compose.test.yml up -d
```

Serviços:
- Redis: `localhost:6379`
- Mailpit UI: `http://localhost:8025`

Para derrubar:
```bash
docker compose --env-file .env.test -f docker-compose.test.yml down
```

### Subir ambiente de deploy (com SMTP externo)
```bash
cp .env.deploy.example .env.deploy
# edite .env.deploy com os dados reais de SMTP e domínio
docker compose --env-file .env.deploy -f docker-compose.deploy.yml up -d --build
```

Para derrubar:
```bash
docker compose --env-file .env.deploy -f docker-compose.deploy.yml down
```
