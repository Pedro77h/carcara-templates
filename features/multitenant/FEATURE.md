# feature: multitenant

Isolamento de dado por tenant com o `@TenantId` nativo do Hibernate: uma coluna `tenant_id`, um
banco, e o filtro aplicado pelo próprio Hibernate em toda leitura e escrita.

## O que entra

| Arquivo | Papel |
|---|---|
| `config/tenant/TenantContext` | tenant da thread; `NONE` por default, `runAs`/`runAsRoot` para jobs |
| `config/tenant/TenantIdentifierResolver` | entrega o tenant ao Hibernate; `ROOT` enxerga todos |
| `config/tenant/TenantInterceptor` + `TenantWebConfig` | decide o tenant da requisição HTTP (vínculo do usuário, ou `X-Tenant-Id` para admin da plataforma) |
| `modules/tenant/` | `UserTenant` (vínculo usuário → tenant), repository, service, component |
| `db/migration/V0.0.5__create_user_tenants_table.sql` | tabela do vínculo |
| `docs/arquitetura/MULTITENANT.md` | o guia que vai para o projeto gerado |
| `appends/docs/arquitetura/{ARQUITETURA,ENTIDADES}.md` | seção curta + tabela `user_tenants` |

Não substitui nenhum arquivo do base. Não traz entidade dona do tenant: no imob é `Agency`, no SMC
é `City` — isso é domínio, e a skill pergunta.

## Por que não é o desenho do imob

O imob liga um `@Filter` do Hibernate num `HandlerInterceptor`. Isso só funciona com
*open-in-view* ligado; o base desliga (`spring.jpa.open-in-view=false`), e aí o filtro cai numa
sessão descartável e **não filtra nada, sem erro nenhum**. Além disso, `@Filter` não se aplica a
`findById`. O `@TenantId` resolve os dois: o tenant é lido quando a sessão abre e vale para toda
carga de entidade.

## Decisões

- **Falha fechada.** Sem tenant definido o valor é `NONE` (`0`): nenhuma linha aparece e o insert
  quebra no `CHECK (tenant_id > 0)`. Porta nova esquecida (gRPC, job, consumidor) não vaza.
- **Vínculo em tabela própria**, não coluna em `users`. Assim a feature não substitui o `User.java`
  do base, que continua evoluindo sem cópia divergente.
- **Admin da plataforma** = `ROLE_ADMIN` sem vínculo. Admin com vínculo é admin do tenant e fica
  preso a ele.

## Testes

`TenantIsolationTest` usa uma entidade **só de teste** (`TenantNote`, tabela criada em
`migration-test`) e prova: insert carimba o tenant, outro tenant não vê nem por `findById`, sem
tenant não vê nem grava, `ROOT` vê tudo.

`TenantInterceptorFlowTest` atravessa o caminho real — JWT → interceptor → transação → Hibernate —
com MockMvc: membro preso ao próprio tenant mesmo mandando outro cabeçalho, admin de tenant idem,
admin da plataforma escolhe por cabeçalho ou vê tudo, cabeçalho inválido dá `400`, usuário sem
vínculo não vê nada, e o tenant não vaza para a thread depois da requisição.

Os dois foram checados por mutação: forçar o resolver a `ROOT` derruba 10 de 10, e o interceptor
deixar de definir o tenant derruba 3 do fluxo.

## Com outras features

- **`grpc`**: a porta gRPC não define tenant → chamadas caem em `NONE`. Precisa de um
  `ServerInterceptor` equivalente antes de expor dado de tenant por gRPC.
- **`storage`**: use o tenant no prefixo da chave (`tenants/{id}/...`). O armazenamento não sabe
  de tenant; o isolamento do arquivo vem de a entidade que guarda a `key` ser `@TenantId`.
