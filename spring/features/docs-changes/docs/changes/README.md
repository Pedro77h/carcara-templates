# Controle de changes

Registro de mudanças no estilo spec-driven (inspirado no OpenSpec). Cada mudança relevante de
comportamento nasce aqui como proposta, com o delta do que muda nas specs vigentes. Abra e arquive
com `/carcara:openspec`.

```
docs/
  specs/<capacidade>/spec.md        # specs vigentes, por capacidade
  changes/<id>/                     # change aberta
    proposal.md  design.md  tasks.md  specs/<capacidade>/spec.md (delta)
  changes/archive/<id>/             # changes entregues e dobradas nas specs
```

1. **Propor**: `changes/<id>/` com proposal, tasks e o delta. Id kebab-case descritivo.
2. **Implementar**: marcar as tasks; o código reflete o delta.
3. **Arquivar**: dobrar o delta em `docs/specs/` e mover a pasta para `changes/archive/`.

Delta: `## ADICIONADO Requisito: <nome>`, `## ALTERADO Requisito: <nome>` (o novo estado completo) ou
`## REMOVIDO Requisito: <nome>`, cada requisito com ao menos um `#### Cenário: <nome>` em
Dado/Quando/Então.

Decisões de arquitetura continuam em `docs/arquitetura/changes/`; este diretório registra
**comportamento**.
