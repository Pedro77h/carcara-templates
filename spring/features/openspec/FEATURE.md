# feature: openspec

Mudanças de comportamento registradas com OpenSpec (CLI `openspec`, `schema: spec-driven`): cada
change é uma pasta com proposta, design, tarefas e o delta das specs, validada pelo CLI e dobrada
nas specs vigentes ao arquivar. Exclusiva com `docs-changes`, a variante sem CLI.

Não confundir com `docs/arquitetura/changes/` do base: lá ficam decisões de arquitetura e de regra
(contexto, decisão, consequência); aqui, o comportamento que o produto passa a ter.

## O que entra

| Arquivo | Papel |
|---|---|
| `openspec/config.yaml` | schema, contexto do projeto e regras por artefato, lidos pelo CLI e pelas IAs |
| `openspec/specs/` | specs vigentes, vazias até a primeira change arquivada |
| `appends/AI_RULES.md`, `appends/CLAUDE.md`, `appends/README.md` | o processo de change para quem trabalha no projeto |

O pre-push não roda `openspec validate` (o CLI é Node e o projeto é Java). Rode à mão ou no CI do
projeto, se quiser a porta.
