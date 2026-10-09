# Entidades

> Uma seção por entidade JPA: a tabela, os campos que importam para a regra, os índices e as
> restrições que o banco garante. Atualize junto com a migration.

## `users`

| Coluna | Tipo | Nota |
|---|---|---|
| `id` | `BIGINT` identity | |
| `full_name` | `VARCHAR(255)` | obrigatório |
| `email` | `VARCHAR(255)` | obrigatório, **único**; login é por aqui |
| `phone` | `VARCHAR(20)` | opcional |
| `password` | `VARCHAR(255)` | hash BCrypt; nunca sai pela API |
| `roles` | `VARCHAR(255)` | lista serializada como `{ROLE_A,ROLE_B}` via `RoleListConverter` |
| `principal_role` | `VARCHAR(255)` | obrigatório, com `CHECK` nos valores do enum `Role` |
| `active` | `BOOLEAN` | scheduler desativa conta sem login há `app.users.inactivity-months` |
| `session_version` | `INTEGER` | vai no JWT; incrementar revoga todas as sessões |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | |
| `password_changed_at` | `TIMESTAMPTZ` | token emitido antes disso é recusado |
| `email_verified_at` | `TIMESTAMPTZ` | nulo = login bloqueado quando a verificação é exigida |
| `last_login_at` | `TIMESTAMPTZ` | indexado; base do scheduler de inatividade |

Ao adicionar role nova ao enum `Role`, **a constraint `CHECK` precisa de migration** — senão o
insert falha em runtime.

## `password_reset_tokens` e `email_verification_tokens`

Mesma forma nas duas: `token_hash` (SHA-256 do token enviado, **único** — o token em claro nunca é
persistido), `user_id` (FK), `expires_at`, `used_at`, `created_at`.

Regras: pedir um token novo marca os pendentes do usuário como usados; validar consome
(`used_at`); scheduler apaga expirado e usado há mais de 24h.

## `auth_attempts`

`scope` (enum `AuthAttemptScope`: login/reset × e-mail/origem), `identifier` (HMAC-SHA256 do
e-mail ou do IP — nunca em claro), `attempted_at`.

Índices: `(scope, identifier, attempted_at)` para a contagem da janela e `(attempted_at)` para a
limpeza. Scheduler apaga tentativa mais velha que `app.auth.rate-limit.retention-hours`.
