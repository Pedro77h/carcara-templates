# API

> Documento vivo consumido pelo time de front. **Todo endpoint novo ou alterado entra aqui no
> mesmo PR** (regra 10 do `CLAUDE.md`), com: método + caminho, quem pode chamar, campos e
> validações do corpo, resposta de sucesso com status, erros de negócio e `curl` de exemplo.
>
> Swagger UI complementa este documento em `/swagger-ui.html` (ligado nos perfis `local` e `dev`).

## Convenções

- Base: `/api`
- Autenticação: `Authorization: Bearer <accessToken>`. Toda rota exige token, exceto as marcadas
  como públicas.
- Erro de validação (`@Valid`) responde `400` com o relatório padrão do Spring.
- Erro de negócio responde o status definido no advice do módulo, com a mensagem no corpo.
- `401` é falta/invalidez de token; `403` é falta de role. Dado de outro usuário responde `404`,
  nunca `403` — não confirmamos a existência do registro alheio.

## Health

### `GET /api/ping` — pública

Prova de vida do serviço.

```bash
curl http://localhost:8080/api/ping
```

```json
{ "status": "UP", "service": "core", "timestamp": "2026-01-01T12:00:00Z" }
```

## Autenticação

As rotas de `/api/auth/**` chegam com a feature `rest` do template. Documente-as aqui ao gerar o
projeto com ela.
