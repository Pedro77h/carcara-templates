# features

Cada pasta aqui é um recorte opcional do template. A skill de geração pergunta quais entram; o
`base/` entra sempre.

## Anatomia de uma feature

A pasta **espelha a raiz do projeto gerado**. `features/rest/src/main/java/...` vai para
`src/main/java/...`, `features/deploy/deploy/` vai para `deploy/`, e assim por diante.

Quatro nomes no topo da feature são **reservados** e nunca são copiados para o projeto:

| Nome reservado | Efeito |
|---|---|
| `FEATURE.md` | documentação da feature, lida por humanos e pela skill |
| `pom-snippet.xml` | inserido no `pom.xml` do base, nos marcadores `<!-- features:dependencies -->`, `<!-- features:test-dependencies -->` e `<!-- features:plugins -->` |
| `properties-snippet.properties` | concatenado no fim de `src/main/resources/application.properties` |
| `appends/` | cada arquivo é **concatenado** no fim do arquivo de mesmo caminho no projeto (ex.: `appends/README.md` → fim do `README.md`) |

Arquivo da feature que já existe no base **substitui** o do base. Use com parcimônia e deixe
registrado no `FEATURE.md` — substituição silenciosa de arquivo é difícil de depurar depois.

## Features

| Feature | O que acrescenta | Substitui |
|---|---|---|
| `rest` | controllers de `auth` e `/api/me`, `AuthAdvice`, rotas públicas de `/api/auth/**`, `scripts/smoke.sh` | `config/security/SecurityConfig.java`, `docs/arquitetura/API.md` |
| `deploy` | EC2 + systemd + nginx + workflow de deploy, `DEPLOY.md` | — |

## O que o base sempre traz

JPA, Security com JWT (access + refresh, `issuer` validado, revogação por `sessionVersion`),
Validation, OpenAPI/Swagger, Flyway com os quatro diretórios de migration, perfis
`local`/`dev`/`prod`, JaCoCo com meta de 80%, os módulos `auth`/`user`/`email`/`health`, a camada
de governança de IA (`AI_RULES.md` + as três réplicas, `CLAUDE.md`, `docs/`, a skill
`revisao-arquitetural`), o hook de `pre-push` e o workflow `pr-review.yml`.

Mais os três documentos de processo: `docs/validacao/VALIDACAO.md` (validação proporcional ao risco e a regra
de validação e2e por change), `docs/achados/REGISTRO_ACHADOS.md` (achados abertos, com porta de entrada
estreita e teto) e `docs/achados/REGISTRO_ACHADOS_ARQUIVO.md` (acervo imutável).
