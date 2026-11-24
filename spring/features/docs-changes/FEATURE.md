# feature: docs-changes

Mudanças de comportamento registradas em `docs/changes/`, no estilo do OpenSpec mas todo em
português e sem CLI (`ADICIONADO/ALTERADO/REMOVIDO Requisito`, cenários Dado/Quando/Então). Quem
valida e arquiva é o `/carcara:openspec`. Exclusiva com `openspec`, a variante com CLI.

Não confundir com `docs/arquitetura/changes/` do base: lá ficam decisões de arquitetura e de regra;
aqui, o comportamento que o produto passa a ter.

## O que entra

| Arquivo | Papel |
|---|---|
| `docs/changes/README.md` | a convenção: estrutura, fluxo e formato do delta |
| `docs/specs/` | specs vigentes, vazias até a primeira change arquivada |
| `appends/AI_RULES.md`, `appends/CLAUDE.md`, `appends/README.md` | o processo de change para quem trabalha no projeto |
