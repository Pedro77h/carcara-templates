# carcara-templates

Templates de projeto do Carcará. Cada pasta da raiz é um template independente, com o próprio
gerador; quem os usa normalmente é o comando `carcara-new-project` do
[carcara-skills](https://github.com/Pedro77h/carcara-skills), que pergunta qual template usar,
gera o projeto e entrevista as regras de negócio.

| Template | O que é |
|---|---|
| [`spring/`](spring/README.md) | Backend Spring Boot 4 / Java 21 IA-first: JPA, Security/JWT, Flyway, OpenAPI e features opcionais (REST, gRPC, Kafka, storage, multi-tenant, deploy) |

## Anatomia de um template

```
<template>/
├── README.md     como gerar e como mexer no template
├── base/         o que todo projeto gerado recebe
├── features/     recortes opcionais
└── scripts/      gerador não interativo + verificação
```

O contrato com o `carcara-new-project` é o gerador não interativo (`scripts/generate.sh` no
`spring`) e os marcadores `<!-- skill:... -->` onde a skill escreve a entrevista. Template novo
segue a mesma anatomia, ganha um workflow `.github/workflows/<template>-matrix.yml` e um guia em
`carcara-skills/skills/carcara-new-project/templates/<template>.md`.
