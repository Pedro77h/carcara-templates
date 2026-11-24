
## 📐 Mudanças de comportamento (OpenSpec)

- Toda mudança de comportamento nasce como change em `openspec/changes/<id>/` (proposal, design
  quando há decisão técnica, tasks e o delta em `specs/<capacidade>/spec.md`), aberta com
  `/carcara:openspec` e validada com `openspec validate <id> --strict`
- A spec vigente fica em `openspec/specs/`; ela só muda ao arquivar a change
  (`/carcara:openspec arquivar <id>`, que roda `openspec archive`)
- Decisões de arquitetura continuam em `docs/arquitetura/changes/`; regra nova continua entrando em
  `docs/REGRAS_DE_NEGOCIO.md` no mesmo PR
- **Ação da IA:** Apontar quando um PR muda comportamento sem change aberta, ou quando o código
  diverge do delta da change
