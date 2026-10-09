# core

Backend Spring Boot gerado a partir do [template](https://github.com/Pedro77h/spring-template).

> **Antes de escrever código, leia `docs/`.** É lá que mora o contexto que o código não conta.

| Documento | Para quê |
|---|---|
| [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) | Camadas, **direção de dependência** (regra dura), ciclo de bean, transação, schema |
| [`docs/REGRAS_DE_NEGOCIO.md`](docs/REGRAS_DE_NEGOCIO.md) | O que o produto faz e **por quê** |
| [`docs/API.md`](docs/API.md) | Contrato das rotas — atualizado no mesmo PR do endpoint |
| [`docs/ENTIDADES.md`](docs/ENTIDADES.md) | Tabelas, campos, índices e restrições |
| [`docs/DEBITO_TECNICO.md`](docs/DEBITO_TECNICO.md) | O que foi deixado para depois, e por quê |
| [`docs/changes/`](docs/changes/) | Histórico de decisões de arquitetura e de regra |
| [`AI_RULES.md`](AI_RULES.md) | Regras para assistentes de código (fonte de verdade) |
| [`CLAUDE.md`](CLAUDE.md) | Recorte dessas regras para o Claude Code |

## Stack

- Java 21 + Spring Boot 4 (Web MVC, Data JPA, Security, Validation, Mail)
- PostgreSQL com schema versionado via Flyway (`ddl-auto=validate`)
- JWT stateless (access + refresh, HS256, `issuer` validado), BCrypt para senhas
- springdoc-openapi — Swagger UI em `/swagger-ui.html`
- Testes: JUnit 5 + BDDMockito + AssertJ, cobertura via JaCoCo (meta ≥80%)

## Rodando localmente

```bash
cp .env.example .env          # ajuste os valores se precisar
docker compose up -d          # sobe o PostgreSQL na porta 5432
./mvnw spring-boot:run        # sobe a API na porta 8080 (perfil local)
```

Checagem rápida: `curl http://localhost:8080/api/ping`.

O perfil `local` carrega `db/migration` + `db/migration-dev`, então o banco já nasce com um
usuário administrador semeado (`SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD_HASH` no `.env`).

## Perfis

| Perfil | Quando | Particularidade |
|---|---|---|
| `local` | máquina do dev | segredo JWT com default, seed de demonstração, Swagger ligado, log `DEBUG` |
| `dev` | ambiente compartilhado | tudo por variável de ambiente, seed de demonstração, Swagger ligado |
| `prod` | produção | tudo obrigatório por ambiente, **sem** seed, Swagger desligado por default |

Troca por `SPRING_PROFILES_ACTIVE`. Em `prod`, `JWT_SECRET` e `PSEUDONYM_SECRET` **precisam** vir
do ambiente — a aplicação recusa subir sem eles.

## Testes

```bash
./mvnw test                   # suíte + relatório JaCoCo em target/site/jacoco/
./mvnw verify                 # o que o CI roda
```

Os testes de integração sobem H2 em modo PostgreSQL com Flyway de verdade (banco isolado por
classe, via `support/CoreTestServer`) — não precisam do Docker no ar.

## Hook de pre-push

```bash
git config core.hooksPath .githooks
```

Antes de cada push, o hook confere: nome das migrations, migration já aplicada que foi alterada,
`./mvnw verify` e cobertura ≥80%. Para pular em emergência: `SKIP_PREPUSH=1 git push`.

## Autenticação

Fluxo JWT stateless. O access token carrega `userId`, `roles`, `tokenType` e `sessionVersion`;
o filtro revalida o usuário no banco a cada requisição, então **roles nunca são confiadas a partir
do cliente**. Incrementar `sessionVersion` (troca de senha, logout) invalida todos os tokens já
emitidos. Rotas e corpos em [`docs/API.md`](docs/API.md).
