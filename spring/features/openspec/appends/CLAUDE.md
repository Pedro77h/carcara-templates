
## Mudanças (OpenSpec)

Mudança de comportamento começa por `/carcara:openspec <pedido>`: a change fica em
`openspec/changes/<id>/` e passa em `openspec validate <id> --strict` antes de implementar. Ao
entregar, `/carcara:openspec arquivar <id>` escreve o Relatório final e dobra o delta em
`openspec/specs/`.
