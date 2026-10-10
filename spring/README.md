# spring

Template de backend Spring Boot 4 / Java 21 **IA-first**: um `base/` que sempre entra e `features/`
opcionais, combinados por `scripts/generate.sh`. Normalmente quem chama o script é o comando
`carcara-new-project` (repo `carcara-skills`), que também entrevista as regras de negócio e
preenche `docs/REGRAS_DE_NEGOCIO.md`.

## Gerar um projeto

Comandos a partir desta pasta (`spring/`):

```bash
scripts/generate.sh \
  --name minha-loja \
  --group br.com.acme \
  --package br.com.acme.loja \
  --features rest,kafka \
  --out ../minha-loja
```

| Parâmetro | Vira |
|---|---|
| `--name` | `artifactId`, `spring.application.name`, banco local, issuer do JWT, nome do serviço no deploy; `MinhaLojaApplication` |
| `--group` | `groupId` |
| `--package` | pacote Java (os diretórios são movidos) |
| `--features` | lista separada por vírgula; vazio = só o base |
| `--no-git` | não faz `git init` nem o commit inicial |

O projeto sai com `git init`, `core.hooksPath=.githooks` e um commit inicial — o `pre-push` já vale
no primeiro push.

## O que vem

- **`base/`** — sempre. JPA, Security/JWT, OpenAPI, Flyway, perfis, módulos `auth`/`user`/`email`/
  `health`, e a camada de governança de IA (`AI_RULES.md` + réplicas, `CLAUDE.md`, `docs/`). Ver
  `base/README.md`.
- **`features/`** — opcionais: `rest`, `grpc`, `kafka`, `storage`, `multitenant`, `deploy`. O
  que cada uma traz, a convenção de pasta e o formato dos snippets estão em
  [`features/README.md`](features/README.md); o porquê de cada uma no `FEATURE.md` dela.

## Mexendo no template

`base/` é um projeto Maven de verdade: `cd base && ./mvnw verify`. Uma feature só se testa
combinada, então a prova é a matriz:

```bash
scripts/verify-matrix.sh            # gera 6 combinações e roda o pre-push de cada uma
```

É o mesmo que roda no CI (`.github/workflows/spring-matrix.yml`, na raiz do repo). Feature nova entra na lista `COMBOS`
do script, sozinha e combinada com as que ela toca.

O nome `core` e o pacote `br.com.template.core` do base são os tokens que o `generate.sh`
substitui. A troca é por padrão pontual, não por palavra solta — `core/utils`, `core.hooksPath` e
`flyway-core` não podem mudar. Ao introduzir um uso novo do nome do projeto, prefira
`${spring.application.name}`; se precisar ser literal, confira que a matriz gera sem sobra
(`grep -rnw core` no projeto gerado).
