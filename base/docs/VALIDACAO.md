# Validação

Como provar que uma mudança funciona, sem rodar o mundo inteiro a cada vírgula. Duas regras
convivem aqui:

1. **Validação proporcional ao risco** — escolha a menor validação segura para o escopo.
2. **Change implementada exige validação e2e do fluxo** — e nenhuma porta automática de lint,
   tipo, build ou teste unitário substitui isso.

## Por que a segunda regra existe

`./mvnw verify` não abre a aplicação. Ele compila, roda mock e sobe H2 — não prova que uma pessoa
consegue percorrer o fluxo contra o banco, o Flyway, o proxy e a configuração reais. Um fluxo pode
estar verde no teste e quebrado em runtime por migration fora de ordem, variável de ambiente
ausente, cabeçalho de proxy não encaminhado ou segredo com default de desenvolvimento.

Por isso: **mudança que altera comportamento de fluxo termina com a prova de que o fluxo roda**,
no nível mais alto disponível no projeto.

## Níveis de validação

### Baixo risco

Documentação, texto, mensagem, comentário, exemplo, seed de desenvolvimento que não muda schema.

- Normalmente **nenhum** teste automatizado.
- Revisar o diff.
- Dizer explicitamente que não rodou teste, e por quê.

### Médio risco

Service ou component localizado, DTO não compartilhado, mapper, ajuste de query sem mudança de
contrato.

- `./mvnw test -Dtest=<ClasseAlterada>Test` — teste do que mudou.
- `./mvnw -q compile` se mexeu em assinatura que outros pacotes usam.
- Teste manual ou `curl` do caminho afetado, quando há rota.

### Alto risco

Autenticação, JWT, `sessionVersion`, roles e autorização, multi-tenancy, migration ou entidade,
`SecurityConfig`, configuração global, contrato de API consumido pelo front, scheduler, fila,
publicação de evento, qualquer coisa em `config/`.

- `./mvnw verify` completo.
- Teste de fluxo (`*FlowTest`) cobrindo o caminho novo **e** o caminho de erro.
- **Validação e2e contra a aplicação de pé** (seção abaixo).
- Conferir log: nenhum dado pessoal, token ou segredo vazando.

Migration **sempre** é alto risco: `ddl-auto=validate` derruba o boot na divergência, e migration
já aplicada não pode ser editada.

## Validação e2e do fluxo

Ordem de preferência, do mais forte para o mais fraco. Use o mais forte que o projeto tiver:

1. **Smoke contra a aplicação de pé** — sobe Postgres e a aplicação, percorre o fluvo crítico por
   HTTP de verdade. É a única prova que inclui Flyway real, perfil, variável de ambiente e proxy.
   Com a feature `rest`: `scripts/smoke.sh` (ver `docs/API.md`).
2. **`*FlowTest`** — contexto Spring completo, H2 em modo PostgreSQL com Flyway rodando, chamadas
   pela porta de entrada. Pega quebra de contrato, autorização e transação; **não** pega problema
   de ambiente nem diferença de dialeto do Postgres.
3. **`*IntegrationTest`** — repositório e query contra banco real de teste.
4. **Teste unitário com mock** — prova a regra, não o fluxo. Nunca é suficiente sozinho para
   mudança de alto risco.

Fluxo novo no produto entra com um `*FlowTest` próprio no mesmo PR. Fluxo que deixou de ser
coberto por um teste removido é regressão, não limpeza.

## Como relatar

**Nunca invente resultado.** Se a validação for pesada, indisponível ou impossível no ambiente,
diga o que foi e o que não foi validado:

```md
## Validações executadas

- `./mvnw verify` — 187 testes, 0 falhas, cobertura 94.7%
- `scripts/smoke.sh` contra local — fluxo de cadastro→login→troca de senha ok

## Não validado

- Rate limit por origem atrás de proxy: exige nginx no caminho, não reproduzido localmente.
  Comando recomendado em homologação: `scripts/smoke.sh` com `API_URL` apontando para o ambiente.

## Risco residual

- Baixo. O caminho não validado é o encaminhamento de cabeçalho, coberto por configuração que não
  mudou neste PR.
```

Três erros de validação que custam caro, os três por terminarem em achado falso sobre o produto:

1. **Medir a versão errada** — validar antes do deploy concluir mede o commit anterior.
2. **Ler cache de cliente como falha** — confira que a resposta vem da versão nova (`/api/ping` e o
   log do serviço) antes de concluir que a feature não chegou.
3. **Confiar em porta que só relata** — workflow que não bloqueia não é prova de nada; confira o
   resultado.

## Portas automáticas deste repositório

| Porta | Onde | Bloqueia? | Alcance |
|---|---|---|---|
| `.githooks/pre-push` | máquina de quem publica, se instalou o hook | **Sim** | migrations, `verify`, cobertura ≥80%, registro de achados |
| `.github/workflows/pr-review.yml` | GitHub Actions, em PR | **Não** | build, cobertura, e comentário com pontos de atenção |

**Nenhuma das duas abre a aplicação.** Essa prova é a validação e2e acima, e ela é sua.

Para instalar o hook: `git config core.hooksPath .githooks`. Pular em emergência:
`SKIP_PREPUSH=1 git push` — e dizer no PR que pulou.
