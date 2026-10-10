
## `user_tenants` (feature `multitenant`)

| Coluna | Tipo | Nota |
|---|---|---|
| `user_id` | `BIGINT` PK | FK `users(id)` `ON DELETE CASCADE` |
| `tenant_id` | `BIGINT` | obrigatório, `CHECK (tenant_id > 0)`, indexado; FK para a tabela dona do tenant fica a cargo do domínio |

Sem linha aqui, o usuário é da plataforma. Ver `MULTITENANT.md`.
