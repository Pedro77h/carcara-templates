# CLAUDE.md

Regras do repositório para o Claude Code. Fonte de verdade completa: `AI_RULES.md` (a mesma política existe replicada em `.cursor/ai_rules.json`, `.ag/ai_rules.toml` e `.github/ai-rules.yml` para outros assistentes — não editar uma sem editar as outras).

**Antes de escrever código, leia `docs/`** — o índice está no `README.md`. Em especial `docs/arquitetura/ARQUITETURA.md` (direção de dependência entre camadas) e `docs/REGRAS_DE_NEGOCIO.md` (o que o produto faz e por quê).

## Domínio do produto

<!-- skill:dominio:inicio -->
<!--
    Esta seção é preenchida pela skill de geração do projeto, a partir da
    entrevista de regras de negócio. Descreva aqui:

    - o que o produto faz, em uma frase
    - os conceitos centrais (entidades) e o que cada um significa no negócio
    - os atores e o que cada role pode fazer
    - o fluxo principal, ponta a ponta
    - a fase atual (PoC, MVP, produção) e o que está deliberadamente fora de escopo

    Enquanto não houver conteúdo aqui, a referência é docs/REGRAS_DE_NEGOCIO.md.
-->

Stack: Spring Boot 4, Java 21, PostgreSQL, schema versionado via **Flyway** (`src/main/resources/db/migration`), JWT stateless (access + refresh, `JwtAuthenticationFilter`).

Módulos implementados até agora: `auth`, `user`, `email`, `health`.
<!-- skill:dominio:fim -->

## Regras obrigatórias

1. **Escrever testes unitários** (JUnit 5 + BDDMockito + AssertJ, estilo given/when/then) para código novo/alterado em `services`/`components`/`mappers`. Manter cobertura de linha ≥80% (checada via JaCoCo, reportada no PR — ver `.github/workflows/pr-review.yml`).
2. **Não instalar novas dependências** (Maven/`pom.xml` ou outras) sem aprovação explícita do mantenedor. Se notar um import que exigiria uma lib não presente no `pom.xml`, avisar antes de adicionar.
3. **Consultar no banco, nunca em memória**, sempre centralizado em `repositories`. **Consulta fixa** usa `@Query` com JPQL; **filtro opcional ou combinável** (busca, chips, intervalo de datas) usa `Specification` — repositório estende `JpaSpecificationExecutor`, o **mapper** monta com `Specification.allOf(...)` e os helpers de `core/utils/SpecificationUtils`, o service só executa. Cadeia de `(:param IS NULL OR ...)` crescendo na query é o sinal de que passou da hora de trocar.
4. **Evitar N+1**: usar fetch joins (`JOIN FETCH`) ou `@EntityGraph`; documentar trade-offs de lazy loading/caching.
5. **Ser conciso**: minimizar tokens em prompts e respostas, focar no contexto relevante.
6. **Mudanças de schema só via migration Flyway** (`src/main/resources/db/migration/V*.sql`), nunca depender de `ddl-auto` (que está em `validate`). Migration já aplicada nunca é editada — o checksum quebra em produção; corrija com uma migration nova. Dado de demonstração vai em `db/migration-dev`.
7. **Usar `Objects.isNull(x)`/`Objects.nonNull(x)`** em vez de `x == null`/`x != null`.
8. **Nunca escrever comentários no código** (nem `//` nem `/** */`), incluindo testes — nomes de classes/métodos/variáveis devem se auto-explicar. Exceção: Javadoc exigido por ferramentas externas (ex: descrições de `@Schema`/`@Operation` do OpenAPI, que são conteúdo voltado ao usuário final, não comentário de código).
9. **Tratar dado pessoal com cuidado**: todo campo pessoal novo precisa de finalidade declarada; se o fluxo funciona sem o dado, não colete. Dado pessoal, token e credencial nunca vão para log, nem em `DEBUG`, nem em mensagem de exceção — logs citam só identificadores técnicos (`user.getId()`, nunca `user.getFullName()`). Consulta de dado de usuário sempre escopada pelo dono na própria query, com `404` (nunca `403`) para registro de terceiro. Nada em query param ou path de URL que não possa aparecer no `access_log` do nginx.
10. **Manter `docs/arquitetura/API.md` atualizado**: todo endpoint novo ou alterado entra nesse documento vivo no mesmo PR — método, autorização, campos e validações, resposta de sucesso, erros de negócio e `curl` de exemplo.
11. **Usar builder pattern (Lombok `@Builder`), nunca `record`**, para DTOs de resposta e objetos de conversão retornados por `mappers`/`services` (ex: resultado de `toXxx(...)`). Padrão: `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`, campos privados, construção via `.builder()...build()`. DTOs de request (corpo de requisição validado com `@Valid`) continuam como `record`.
12. **Validar proporcionalmente ao risco, e provar o fluxo ponta a ponta** (`docs/validacao/VALIDACAO.md`): mudança de baixo risco não precisa de teste, mudança de alto risco (auth, roles, migration, `config/`, contrato de API, scheduler) exige `./mvnw verify` **mais** validação e2e do fluxo — `./mvnw verify` não abre a aplicação. Nunca inventar resultado de validação: dizer o que rodou, o que não rodou, o comando recomendado e o risco residual.
13. **Registrar achado em `docs/achados/REGISTRO_ACHADOS.md`**, com a porta de entrada estreita: só entra achado de severidade alta/crítica, de origem externa (usuário, homologação, teste que falhou, porta de qualidade) ou pedido explicitamente. Achado baixo/médio que você mesmo encontrou revisando seu próprio trabalho **não entra** — corrija na hora ou descarte, e nada corrigido dentro da mesma tarefa vira achado (o commit já é o rastro). Achado encerrado sai do registro vivo e vai para o acervo; defeito que volta é achado novo citando o arquivado, nunca reabertura.

## Papel em revisão de código

Ao revisar código (PRs, diffs), apontar problemas como observações/comentários — nunca aprovar ou reprovar PRs. Aprovação/reprovação é responsabilidade exclusiva de mantenedores humanos.

## Arquitetura do projeto

Spring Boot (Java) em `src/main/java/br/com/template/core/`, organizado por módulo:

- `modules/<nome>/controller` — porta de entrada (REST, gRPC, SSE)
- `modules/<nome>/services` — lógica de negócio
- `modules/<nome>/components` — fachada do módulo para consumo por outros módulos
- `modules/<nome>/repositories` — acesso a dados (JPA/JPQL)
- `modules/<nome>/dtos` — transfer objects
- `modules/<nome>/mappers` — mapeamento entidade ↔ DTO
- `modules/<nome>/models` — entidades JPA
- `modules/<nome>/schedulers` — tarefas agendadas do módulo
- `config/security` — JWT, autenticação/autorização
- `core/utils` — utilitários puros compartilhados entre módulos; só entra aqui o que não pertence a um módulo específico
- `core/components` — componentes Spring compartilhados entre módulos; mesma regra do `core/utils`, mas para beans gerenciados
- `core/exceptions` — exceções compartilhadas entre módulos

**Direção das dependências entre camadas** (regra dura, vale para código novo e refatoração — detalhe e armadilhas em `docs/arquitetura/ARQUITETURA.md`):

- `controller` → **só** `components`. Controller nunca injeta service, repository ou mapper.
- `components` → outros `components` e os `services` do próprio módulo. Precisa de algo de outro módulo? Passa pelo `component` daquele módulo, nunca pelo service dele.
- `services` → `repositories` e outros `services`. Service nunca injeta `component`.
- `repositories` → só acesso a dados.

Quando um `service` precisar de algo que hoje é `component` (ou vice-versa), o certo é mover a peça de camada, não furar a regra.

**Cuidado com ciclo de bean.** Se `AComponent` injeta `BComponent`, `BComponent` não pode injetar `AComponent` — o startup quebra. Orquestração que atravessa muitos módulos ganha um componente próprio (padrão do `AccountComponent`, que orquestra a exclusão de conta sem morar dentro do `UserComponent`), e cada módulo continua dono do que apaga (`deleteAllByUserId` no componente e no service dele).

Um módulo que **não é dono de dado** pode não ter service nem repository — pode existir só com controller, component e mapper, compondo outros módulos.

## Autenticação

Fluxo JWT stateless: `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh` (ver `README.md` e `docs/arquitetura/API.md`). Roles nunca são confiadas a partir do cliente — sempre validadas no back-end via banco de dados.

Troca de senha autenticada revoga **todas** as sessões (via `sessionVersion` no token) e devolve um par de tokens novo — o cliente precisa substituir o que guarda.

## Exceções

Novas dependências só são permitidas com Issue documentada (motivo, impacto, implementação proposta) + aprovação do mantenedor.
