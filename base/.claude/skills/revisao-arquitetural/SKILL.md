---
name: revisao-arquitetural
description: Revisa código deste repositório com olhar de arquiteto e engenheiro sênior — code smells, performance, violação das regras do projeto, bugs prováveis, segurança e dado pessoal. Usar em PRs, diffs ou antes de abrir PR ("revisar como arquiteto", "revisão arquitetural", "olhar de sênior").
---

# Revisão arquitetural

Revisar o diff (`git diff origin/main...HEAD` ou a PR indicada). **Nunca aprovar nem reprovar** — só observações. Aprovação é de mantenedor humano (`AI_RULES.md`).

Ler apenas os arquivos tocados e seus vizinhos diretos (chamadores, teste correspondente, migration relacionada). Não auditar o repo inteiro.

## Eixos

**1. Arquitetura e direção de dependência** (regra dura do `CLAUDE.md` e do `docs/arquitetura/ARQUITETURA.md`)
- `controller` → só `components`. Controller injetando service/repository/mapper é violação.
- `components` → components de outros módulos + services do próprio módulo. Nunca service de outro módulo.
- `services` → repositories e outros services. Service nunca injeta component.
- Módulo dono do dado é quem apaga o dado dele (`deleteAllByUserId`). Orquestração multi-módulo em componente próprio (ex: `AccountComponent`) — ciclo de bean quebra o startup.
- Peça no lugar errado: mover de camada, não furar a regra.

**2. Code smells**
- Lógica de negócio em controller ou mapper; service anêmico repassando sem regra.
- Duplicação do que já existe em `core/utils` / `core/components` / componente de outro módulo.
- Abstração especulativa: interface com uma implementação, config para valor que nunca muda, CRUD que ninguém pediu.
- Comentário no código (`//`, `/** */`) — proibido, exceto `@Schema`/`@Operation`.
- `record` em DTO de resposta (tem que ser `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`); `x == null` em vez de `Objects.isNull(x)`.
- Nome que não se explica sozinho (a regra proíbe comentário justamente por isso).

**3. Performance e banco**
- N+1: relação lazy acessada em loop sem `JOIN FETCH` / `@EntityGraph`.
- Filtro em memória (`stream().filter`) sobre o que a query devia filtrar/paginar; `findAll()` sem limite.
- Cadeia de `(:param IS NULL OR ...)` crescendo → trocar por `Specification` com `SpecificationUtils`.
- Leitura seguida de escrita onde precisa de `UPDATE` condicional (reserva de vaga, rate limit, consumo de token) — corrida real.
- Consulta sem índice correspondente na migration.

**4. Segurança e dado pessoal**
- Dado pessoal, token, credencial ou segredo em log, exceção, query param ou path de URL — inclusive indiretamente (`toString()` de entidade, `log.error("...{}", user)`).
- Consulta de dado de usuário não escopada pelo dono **na própria query**; retorno `403` onde deve ser `404`.
- Rota nova sem autenticação fora da lista pública do `SecurityConfig`; `@PreAuthorize`/role ausente em rota administrativa.
- Segredo com default em perfil que não seja `local`.
- Senha/token persistido em claro (tem que ser BCrypt para senha, hash para token de uso único).

**5. Bugs prováveis e mau funcionamento**
- `Optional.get()`/unwrap sem checagem; NPE em campo opcional; divisão/índice sem guarda.
- Fuso e borda de tempo (`OffsetDateTime` vs `LocalDateTime`, janela de expiração, corte de listagem).
- Transação: escrita multi-tabela sem `@Transactional`; `@Transactional` em método privado ou chamado internamente (proxy não intercepta).
- Migration incompatível com a entidade (`ddl-auto=validate` derruba o boot); coluna `NOT NULL` adicionada sem default em tabela com dados; migration já aplicada sendo editada; valor novo de enum sem atualizar a constraint `CHECK`.
- Mudança de contrato: campo de DTO removido/renomeado, status HTTP alterado, validação afrouxada.
- Teste ausente para o caminho de erro, ou teste que só confirma o mock.

**6. Testes e documentação**
- Código novo em `services`/`components`/`mappers` sem teste JUnit 5 + BDDMockito + AssertJ (given/when/then); cobertura ≥80%.
- Endpoint novo ou alterado sem entrada em `docs/arquitetura/API.md` no mesmo PR.
- Regra de negócio nova sem entrada em `docs/REGRAS_DE_NEGOCIO.md`; entidade ou coluna nova sem entrada em `docs/arquitetura/ENTIDADES.md`.
- Dependência nova no `pom.xml` sem aprovação do mantenedor — sinalizar sempre.

## Saída

Agrupar por severidade, mais grave primeiro. Cada achado em uma linha:

`**[Alto|Médio|Baixo]** \`caminho/Arquivo.java:linha\` — o problema, por que quebra, e a correção em uma frase.`

Só reportar o que o diff introduz ou piora. Nada encontrado num eixo: omitir o eixo. Fechar com uma linha de risco geral (ALTO/MÉDIO/BAIXO) e a nota de que a decisão final é do mantenedor.
