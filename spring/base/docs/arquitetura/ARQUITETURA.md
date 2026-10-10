# Arquitetura

Spring Boot (Java 21) organizado **por módulo de negócio**, não por camada técnica. Cada módulo é um pacote em `src/main/java/br/com/template/core/modules/<nome>/` com as suas próprias camadas dentro.

## Camadas de um módulo

```
modules/<nome>/
├── controller/     porta de entrada (REST, gRPC, SSE)
├── components/     fachada do módulo para quem está fora dele
├── services/       lógica de negócio e transação
├── repositories/   acesso a dados (JPA/JPQL/Specification)
├── mappers/        entidade <-> DTO e montagem de Specification
├── dtos/           transfer objects (request e response)
├── models/         entidades JPA e enums de domínio
├── exceptions/     exceções de negócio do módulo
└── schedulers/     tarefas agendadas do módulo
```

Nenhuma dessas pastas é obrigatória. Um módulo que **não é dono de dado** pode existir só com `controller`, `components` e `mappers`, compondo outros módulos.

## Direção das dependências (regra dura)

```
controller  ──►  components  ──►  services  ──►  repositories
                      │               │
                      ▼               ▼
              components de     services do
              outros módulos    próprio módulo
```

| Camada | Pode injetar | Nunca injeta |
|---|---|---|
| `controller` | **só** `components` | service, repository, mapper |
| `components` | outros `components`, `services` do próprio módulo | service de outro módulo, repository |
| `services` | `repositories`, outros `services` | `components` |
| `repositories` | nada | — |

Por quê: o `component` é o único contrato público do módulo. Enquanto todo mundo entra por ele, dá para mudar service, repository e entidade sem caçar chamadores pelo resto do código. Controller que injeta service pula o contrato; service que injeta component inverte a seta e cria ciclo.

Quando um `service` precisar de algo que hoje é `component` (ou vice-versa), **mova a peça de camada — não fure a regra.**

## Ciclo de bean

Se `AComponent` injeta `BComponent`, então `BComponent` **não pode** injetar `AComponent`: o Spring não consegue construir o grafo e o startup quebra.

Duas saídas, nessa ordem de preferência:

1. **Quebrar em dois beans no mesmo módulo** — quem escreve e quem lê viram beans diferentes. Ex.: se `AComponent` empurra dados para o módulo B, a leitura do módulo B vive em `BBacklogComponent`, separado do `BComponent` que A injeta.
2. **Componente de orquestração** — fluxo que atravessa muitos módulos ganha um bean próprio, fora dos componentes que ele coordena. É o papel do `AccountComponent`: ele orquestra exclusão/alteração de conta chamando `UserComponent` e `AuthComponent`, em vez de o `UserComponent` chamar todo mundo e fechar o grafo.

Cada módulo continua **dono do que apaga**: quem remove dados de um módulo é o service/component daquele módulo (`deleteAllByUserId`), chamado pelo orquestrador.

## Compartilhado entre módulos

```
core/
├── utils/        utilitário puro, sem estado (SpecificationUtils, ...)
├── components/   bean Spring compartilhado (CurrentUserComponent, PseudonymHasher)
└── exceptions/   exceção usada por mais de um módulo
```

Só entra em `core/` o que **não pertence a nenhum módulo específico**. Na dúvida, começa no módulo; sobe para `core/` no segundo módulo que precisar.

## Config

```
config/
├── security/            SecurityConfig, PasswordConfig
├── security/jwt/        filtro, provider, propriedades e serviço do token
├── advices/             @RestControllerAdvice: exceção de negócio -> status HTTP
└── openapi/             OpenApiConfig (Swagger UI em /swagger-ui.html)
```

A política do token (assinatura, `issuer`, tipo `access`, usuário existente, corte por troca de
senha, sessão revogada) mora **só** no `JwtAuthenticationProvider`. O `JwtAuthenticationFilter`
apenas extrai o `Bearer` e delega. Toda nova porta de entrada (gRPC, WebSocket, consumidor com
token) reaproveita o provider — nunca reimplementa a validação.

Cada módulo com exceções de negócio ganha o seu próprio advice (`<Modulo>Advice`), que traduz exceção em status. `NotFoundAdvice` é o único global.

## Consulta: JPQL ou Specification

- **Consulta fixa** (sem filtro opcional): `@Query` com JPQL no repositório.
- **Filtro opcional ou combinável** (busca, chips, intervalo de datas): `Specification`. O repositório estende `JpaSpecificationExecutor`, o **mapper** monta a especificação com `Specification.allOf(...)` e os helpers de `core/utils/SpecificationUtils` (que ignoram filtro ausente), e o service só executa.

O sinal de que passou da hora de trocar JPQL por Specification: condições `(:param IS NULL OR campo = :param)` se acumulando na query.

Filtrar, ordenar e paginar é trabalho do banco. `stream().filter(...)` sobre `findAll()` é bug de performance esperando volume.

## Transação

`@Transactional` no **service**, nunca no controller nem no component. Atenção a duas armadilhas:

- `@Transactional` em método privado ou chamado de dentro da própria classe não intercepta (o proxy fica de fora).
- Escrita em mais de uma tabela sem `@Transactional` deixa o banco meio escrito quando falha no meio.

Onde há corrida real (reserva de vaga, rate limit, consumo de token de uso único), use **`UPDATE` condicional** em vez de ler-e-depois-escrever.

## Schema

Flyway é a única via de mudança de schema. `ddl-auto` fica em `validate`: se a entidade e a tabela divergirem, a aplicação **não sobe** — o que é o objetivo.

```
db/migration/        schema da aplicação (V0.0.N__descricao.sql)
db/migration-dev/    seed de demonstração (V0.1.N__...), só nos perfis local e dev
db/migration-prod/   o que é exclusivo de produção
db/migration-test/   o que é exclusivo da suíte de testes
```

**Migration já aplicada nunca é editada** — o checksum quebra em produção. Corrija com uma migration nova.

## Testes

- `*Test` — unitário, com mock (JUnit 5 + BDDMockito + AssertJ, given/when/then).
- `*IntegrationTest` — sobe o contexto e o banco H2 em modo PostgreSQL, com Flyway rodando de verdade. Estende `support/CoreTestServer`, que dá um banco isolado por classe.
- `*FlowTest` — percorre um fluxo de negócio ponta a ponta pela porta de entrada.

Cobertura de linha ≥80%, medida por JaCoCo. O `.githooks/pre-push` barra o push abaixo disso.
