# core

Backend Spring Boot gerado a partir do [template](https://github.com/Pedro77h/carcara-templates/tree/main/spring).

> **Antes de escrever código, leia `docs/`.** É lá que mora o contexto que o código não conta.

### O produto

| Documento | Para quê |
|---|---|
| [`docs/REGRAS_DE_NEGOCIO.md`](docs/REGRAS_DE_NEGOCIO.md) | O que o produto faz e **por quê**. Comece por aqui |

### `docs/arquitetura/` — como o código é organizado

| Documento | Para quê |
|---|---|
| [`ARQUITETURA.md`](docs/arquitetura/ARQUITETURA.md) | Camadas, **direção de dependência** (regra dura), ciclo de bean, transação, schema |
| [`API.md`](docs/arquitetura/API.md) | Contrato das rotas — atualizado no mesmo PR do endpoint |
| [`ENTIDADES.md`](docs/arquitetura/ENTIDADES.md) | Tabelas, campos, índices e restrições |
| [`changes/`](docs/arquitetura/changes/) | Histórico de decisões de arquitetura e de regra |

### `docs/validacao/` — como provar que funciona

| Documento | Para quê |
|---|---|
| [`VALIDACAO.md`](docs/validacao/VALIDACAO.md) | Validação proporcional ao risco e a **validação e2e por change** |

### `docs/achados/` — o que está pendente

| Documento | Para quê |
|---|---|
| [`REGISTRO_ACHADOS.md`](docs/achados/REGISTRO_ACHADOS.md) | Achados **abertos** (bug, gap, risco) e como registrar |
| [`REGISTRO_ACHADOS_ARQUIVO.md`](docs/achados/REGISTRO_ACHADOS_ARQUIVO.md) | Acervo imutável dos achados encerrados |
| [`DEBITO_TECNICO.md`](docs/achados/DEBITO_TECNICO.md) | Atalho deixado de propósito, com o motivo e a saída |

> A diferença entre as duas pastas de `achados/`: **débito** é escolha consciente, **achado** é
> defeito observado. Confundir os dois faz o débito parecer bug e o bug parecer plano.

### Regras para assistentes

| Documento | Para quê |
|---|---|
| [`AI_RULES.md`](AI_RULES.md) | Fonte de verdade, replicada em `.cursor/`, `.ag/` e `.github/` |
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
`./mvnw verify`, cobertura ≥80% e o registro de achados (nada encerrado no arquivo vivo, teto de
abertos respeitado). Para pular em emergência: `SKIP_PREPUSH=1 git push` — e dizer no PR que pulou.

**O hook não abre a aplicação.** Mudança que altera comportamento de fluxo precisa da validação e2e
descrita em [`docs/validacao/VALIDACAO.md`](docs/validacao/VALIDACAO.md).

## Autenticação

Fluxo JWT stateless. O access token carrega `userId`, `roles`, `tokenType` e `sessionVersion`;
o filtro revalida o usuário no banco a cada requisição, então **roles nunca são confiadas a partir
do cliente**. Incrementar `sessionVersion` (troca de senha, logout) invalida todos os tokens já
emitidos. Rotas e corpos em [`docs/arquitetura/API.md`](docs/arquitetura/API.md).
