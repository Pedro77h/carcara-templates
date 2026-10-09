# API

> Documento vivo consumido pelo time de front. **Todo endpoint novo ou alterado entra aqui no
> mesmo PR** (regra 11 do `AI_RULES.md`), com: método + caminho, quem pode chamar, campos e
> validações do corpo, resposta de sucesso com status, erros de negócio e `curl` de exemplo.
>
> Swagger UI complementa este documento em `/swagger-ui.html` (ligado nos perfis `local` e `dev`).

## Convenções

- Base: `/api`
- Autenticação: `Authorization: Bearer <accessToken>`. Toda rota exige token, exceto as marcadas
  como **pública**.
- Erro de validação (`@Valid`) responde `400` com o relatório padrão do Spring.
- Erro de negócio responde o status definido no advice do módulo, com a mensagem no corpo.
- `401` é falta/invalidez de token; `403` é falta de role. Dado de outro usuário responde `404`,
  nunca `403` — não confirmamos a existência do registro alheio.
- Todo token (de redefinição, de confirmação) viaja **no corpo**, nunca em query param ou path: o
  `access_log` do nginx grava a URL inteira ao lado do IP.

## Health

### `GET /api/ping` — pública

```bash
curl http://localhost:8080/api/ping
```

```json
{ "status": "UP", "service": "core", "timestamp": "2026-01-01T12:00:00Z" }
```

## Auth

### `POST /api/auth/register` — pública

| Campo | Tipo | Validação |
|---|---|---|
| `fullName` | string | obrigatório, até 255 |
| `phone` | string | opcional, 10 ou 11 dígitos |
| `email` | string | obrigatório, formato de e-mail |
| `password` | string | obrigatório, mínimo 8 |

**`202 Accepted` sem corpo** é a resposta normal: a conta foi criada e o e-mail de confirmação
enviado. É **também** a resposta quando o e-mail já tem conta — nesse caso nada é criado e o dono
do e-mail recebe um aviso de tentativa. O chamador não consegue distinguir os dois casos, e isso é
deliberado: evita enumeração de contas.

Com `app.auth.email-verification-required=false` a conta nasce confirmada e a resposta é
**`200`** com o par de tokens (ver `/login`). Use só em demonstração.

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Ana Souza","phone":"85999990000","email":"ana@example.com","password":"senha-forte"}'
```

### `POST /api/auth/verify-email` — pública

Corpo: `{ "token": "<token recebido por e-mail>" }` (obrigatório).

`200` com o par de tokens. Erros: `400` token inválido ou já usado, `410` token expirado.

### `POST /api/auth/login` — pública

Corpo: `{ "email": "...", "password": "..." }` (ambos obrigatórios).

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "userId": 7,
  "email": "ana@example.com",
  "fullName": "Ana Souza",
  "principalRole": "ROLE_USER",
  "roles": ["ROLE_USER"]
}
```

Erros: `401` credencial inválida **ou e-mail não confirmado** (a resposta é a mesma de propósito);
`429` tentativas excedidas na janela (`app.auth.rate-limit.*`), por e-mail ou por origem.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","password":"senha-forte"}'
```

### `POST /api/auth/refresh` — pública

Corpo: `{ "refreshToken": "..." }` (obrigatório). `200` com um par novo.

Erros: `401` token inválido, expirado, do tipo errado, emitido antes de uma troca de senha ou de
uma revogação de sessão.

### `POST /api/auth/logout` — autenticada

Sem corpo. `204`. Incrementa o `sessionVersion` do usuário, então **todo token emitido antes deste
instante deixa de valer**, em todos os aparelhos.

```bash
curl -X POST http://localhost:8080/api/auth/logout -H "Authorization: Bearer $ACCESS_TOKEN"
```

### `POST /api/auth/forgot-password` — pública

Corpo: `{ "email": "..." }` (obrigatório).

`200` sem corpo, **exista a conta ou não** — mesma razão do `register`. Limitado por e-mail e por
origem; dentro do bloqueio a resposta continua `200` e nenhum e-mail é enviado.

### `POST /api/auth/forgot-password/valid` — pública

Corpo: `{ "token": "..." }` (obrigatório). `200` quando o token ainda serve.

Erros: `400` inválido ou já usado, `410` expirado. É `POST` com o token no corpo justamente para
ele não acabar no log de acesso do servidor web.

### `POST /api/auth/reset-password` — pública

Corpo: `{ "token": "...", "newPassword": "..." }` (`newPassword` mínimo 8). `200` sem corpo.

Erros: `400` token inválido ou já usado, `410` expirado. O token é de uso único; pedir um novo
invalida os pendentes.

## Perfil e conta

Todas autenticadas, sempre escopadas pelo dono do token — o `userId` **nunca** vem do corpo.

### `GET /api/me`

```json
{
  "id": 7,
  "fullName": "Ana Souza",
  "email": "ana@example.com",
  "phone": "85999990000",
  "principalRole": "ROLE_USER",
  "emailVerified": true,
  "createdAt": "2026-01-01T12:00:00Z"
}
```

### `PATCH /api/me`

| Campo | Tipo | Validação |
|---|---|---|
| `fullName` | string | obrigatório, até 255 |
| `phone` | string | opcional, 10 ou 11 dígitos |

`200` com o perfil atualizado. **E-mail não é editável** — é a chave de login.

### `POST /api/me/password`

Corpo: `{ "currentPassword": "...", "newPassword": "..." }` (`newPassword` mínimo 8).

`200` com um **par de tokens novo**: a troca revoga todas as sessões, inclusive a que fez a
chamada. O cliente precisa substituir o que guarda, senão a próxima requisição toma `401`.

Erros: `401` senha atual incorreta (e nada é alterado).

### `DELETE /api/me`

Corpo: `{ "password": "..." }` (obrigatório). `204`.

Apaga os tokens de autenticação e a linha do usuário. Erros: `401` senha incorreta.

```bash
curl -X DELETE http://localhost:8080/api/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"password":"senha-forte"}'
```
