
## Multi-tenant (feature `multitenant`)

Dado de tenant isola por `@TenantId` do Hibernate, nunca por `WHERE tenant_id` escrito no
repository. O tenant da requisição é decidido no `TenantInterceptor` e guardado em
`TenantContext`; jobs usam `TenantContext.runAs(...)`/`runAsRoot(...)` **antes** da transação.
Sem tenant definido, nada aparece e nada é gravado.

Regras, exceções (SQL nativo, `@Modifying`, unicidade) e o passo a passo para uma entidade nova
em [`MULTITENANT.md`](MULTITENANT.md). Leia antes de criar qualquer tabela de domínio.
