---
alwaysApply: true
appliesTo:
  copilot:
    config: ".github/ai-rules.yml"
    syntax: "yaml"
  claude:
    config: "AI_RULES.md"
    syntax: "markdown"
  cursor:
    config: ".cursor/ai_rules.json"
    syntax: "json"
  codex:
    config: "AI_RULES.md"
    syntax: "markdown"
  antigravity:
    config: ".ag/ai_rules.toml"
    syntax: "toml"

---

# Regras de IA do repositório (alwaysApply: true)

Este documento define regras que assistentes de código (Copilot, Claude, Cursor, Codex, Antigravity) devem seguir ao revisar e interagir com este repositório. A mesma política está replicada em `.cursor/ai_rules.json`, `.ag/ai_rules.toml` e `.github/ai-rules.yml` — **não editar uma sem editar as outras**.

## 🎯 Papel da IA na revisão de código

### ✅ O que a IA DEVE fazer:
- **Apontar pontos de atenção como comentários** no código (não aprova/desaprova)
- Sugerir melhorias em legibilidade, performance, segurança e arquitetura
- Alertar para violações das regras do projeto via comentários inline
- Fornecer contexto e alternativas para decisões de design

### ❌ O que a IA NÃO DEVE fazer:
- ~~Aprovar PRs~~ → Apenas repositórios/branches protegidas fazem isso
- ~~Desaprovar PRs~~ → Apenas mantenedores autorizados fazem isso
- ~~Dar comentário final de "LGTM" ou similar~~ → Isso é responsabilidade dos revisores humanos

## 🛡️ Regras principais (valem sempre)

### 1️⃣ Escreva testes unitários (BDD) para código novo/alterado
- Padrão: JUnit 5 + BDDMockito + AssertJ, estrutura given/when/then, foco em `services`/`components`/`mappers`
- Meta: cobertura de linha ≥80%, medida via JaCoCo e reportada automaticamente no PR
- **Ação da IA:** Apontar via comentário quando um PR reduzir a cobertura abaixo de 80% ou adicionar lógica sem teste correspondente

### 2️⃣ Sempre siga a arquitetura proposta do projeto
- **Preserve a estrutura de pacotes:**
  - `modules/*` → funcionalidades independentes
  - `controller` → endpoints (REST, gRPC, SSE)
  - `components` → fachada do módulo para consumo por outros módulos
  - `services` → lógica de negócio
  - `repositories` → acesso a dados (JPA/JPQL)
  - `dtos` → transfer objects
  - `mappers` → mapeamento entre entidades e DTOs
- **Direção de dependência é regra dura** (detalhe em `docs/ARQUITETURA.md`): `controller` → só `components`; `components` → outros `components` e os `services` do próprio módulo; `services` → `repositories` e outros `services`, nunca `components`
- **Princípios:** SOLID, separação de responsabilidades, DDD quando apropriado
- **Ação da IA:** Apontar como comentário quando a camada ou a direção de dependência for violada

### 3️⃣ Não instale novas bibliotecas sem autorização explícita
- **Regra:** Toda nova dependência requer Issue/PR documentada + aprovação do mantenedor
- **Ação da IA:** Apontar quando houver imports de libs não documentadas no `pom.xml`
- Comentário modelo: "⚠️ Nova dependência detectada: `xxx`. Favor abrir Issue com justificativa antes de merge."

### 4️⃣ Consulta no banco, não em memória — JPQL ou Specification conforme o caso
- **Consulta fixa** (sem filtro opcional): `@Query` com JPQL no repositório
- **Filtro opcional ou combinável** (busca, chips, intervalos de data): `Specification`, com o repositório estendendo `JpaSpecificationExecutor` e o **mapper** montando a especificação via `Specification.allOf(...)` e os helpers de `core/utils/SpecificationUtils`
- O sinal de que passou da hora de trocar JPQL por Specification: condições do tipo `(:param IS NULL OR campo = :param)` se acumulando na query
- Evite: múltiplas queries + processamento em Java
- **Ação da IA:** Sugerir refatoração de queries complexas em Java para JPQL, e de cadeias de `:param IS NULL` para Specification

### 5️⃣ Evite problemas de N+1 e otimize queries
- Use fetch joins: `@Query("SELECT u FROM User u JOIN FETCH u.roles")`
- Use EntityGraphs para consultas dinâmicas
- Documente trade-offs ao usar lazy loading ou caching
- **Ação da IA:** Alertar via comentário possíveis N+1 ou queries ineficientes

### 6️⃣ Otimize o uso de tokens nas IAs
- Seja conciso ao solicitar reviews ou gerar prompts
- Forneça apenas contexto relevante
- **Ação da IA:** Ser direto e objetivo nos comentários de revisão

### 7️⃣ Use `Objects.isNull()`/`Objects.nonNull()` em vez de comparar com null
- Prefira: `Objects.isNull(x)` / `Objects.nonNull(x)`
- Evite: `x == null` / `x != null`
- **Ação da IA:** Apontar via comentário quando encontrar comparação direta com `null` em código novo/alterado

### 8️⃣ Nunca escreva comentários no código-fonte (nem `//` nem `/** */`)
- Nomes de classes/métodos/variáveis devem se auto-explicar; sem comentários explicando "o quê" nem "por quê"
- Vale para código de produção e para testes (nada de comentários marcando given/when/then)
- Exceção: Javadoc exigido por ferramentas externas (ex: descrições `@Schema`/`@Operation` do OpenAPI, conteúdo voltado ao usuário final, não comentário de código)
- **Ação da IA:** Apontar via comentário de PR quando encontrar comentário de código novo/alterado que não se enquadre na exceção acima

### 9️⃣ Mudanças de schema só via migration Flyway
- Toda alteração de tabela/coluna entra em `src/main/resources/db/migration/V*.sql`; nunca depender de `ddl-auto` (que está em `validate`)
- **Migration já aplicada nunca é editada** — o checksum quebra em produção. Corrija com uma migration nova
- Dados de demonstração vão em `db/migration-dev`, nunca em `db/migration`
- **Ação da IA:** Apontar quando um PR alterar entidade JPA sem migration correspondente, ou editar migration já versionada

### 🔟 Trate dado pessoal com finalidade declarada
- Todo campo pessoal novo precisa de finalidade clara; se o fluxo funciona sem o dado, não colete
- Dado pessoal, token e credencial **nunca** vão para log, nem em `DEBUG`, nem em mensagem de exceção — logs de erro citam apenas identificadores técnicos (`user.getId()`, nunca `user.getFullName()`)
- Consulta de dado de usuário sempre escopada pelo dono **na própria query**; registro de terceiro responde `404`, nunca `403`
- Nada em query param ou path de URL que não possa aparecer no `access_log` do nginx
- **Ação da IA:** Apontar quando um PR logar dado pessoal, expor dado de outro usuário, ou coletar campo sem finalidade

### 1️⃣1️⃣ Mantenha `docs/API.md` atualizado a cada endpoint
- `docs/API.md` é documento vivo consumido pelo time de front: todo endpoint novo/alterado entra lá no mesmo PR
- Cada entrada traz: método + caminho, quem pode chamar, campos e validações do corpo, resposta de sucesso com status, erros de negócio e `curl` de exemplo
- **Ação da IA:** Apontar via comentário quando um PR adicionar ou mudar um endpoint sem atualizar `docs/API.md`

### 1️⃣2️⃣ Use builder pattern (Lombok `@Builder`), nunca `record`, para objetos de conversão de DTO
- Vale para DTOs de **resposta**, resultados de mapper (`toXxx`) e objetos de retorno de service que carregam dados de/para outra camada
- Padrão: classe com `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`, campos privados, construção via `.builder()...build()`
- Exceção: DTOs de **request** (corpo de requisição validado com `@Valid`) continuam como `record`
- **Ação da IA:** Apontar quando um mapper/service introduzir um `record` para um objeto de resposta em vez de uma classe com builder

## 📌 Exemplo de comentário da IA (não aprovação)

```java
// ⚠️ N+1 Query Risk: for loop consultando roles por item
List<User> users = userService.getAllUsers(); // ← 1 query
for (User u : users) {
    System.out.println(u.getRoles()); // ← +N queries (N+1)
}

// 💡 Sugestão: Refatore para usar FETCH JOIN
// @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles")
// List<User> findAllWithRoles();
```

## 🚦 Processo de exceções e autorizações

Para autorizar a instalação de novas libs:
1. Abra uma **Issue** com motivo claro, impacto (segurança, desempenho, manutenção) e implementação proposta
2. Obtenha **aprovação de mantenedor** (via label ou comentário)
3. **IA continua apontando** pontos de atenção, não aprova
4. **Merge feito por quem tem permissão** no repositório

## ✨ Notas finais

- **Este arquivo é a fonte de verdade** para comportamento esperado de IAs
- Assistentes sempre apontam pontos de atenção como **comentários**, nunca como aprovação/desaprovação
- **Aprovação de PRs** é responsabilidade exclusiva de mantenedores configurados no branch protection
