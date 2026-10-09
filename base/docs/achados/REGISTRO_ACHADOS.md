# Registro de achados

Registro central de bugs, gaps, inconsistências, riscos operacionais e evidências observadas
durante implementação, revisão, homologação ou operação. É a fonte única para achado pontual —
evita arquivo avulso, evidência perdida e registro duplicado.

**Aqui só existe o que está aberto.** Achado encerrado (`Corrigido`, `Validado`, `Risco aceito`,
`Descartado`) vai para [`REGISTRO_ACHADOS_ARQUIVO.md`](REGISTRO_ACHADOS_ARQUIVO.md), que é
histórico e imutável. Identificador citado em código, teste, CI ou PR resolve em um dos dois:
procure aqui primeiro, depois no acervo.

## Critério de entrada

A porta é estreita de propósito. Só entra achado que satisfaça **uma** destas condições:

1. severidade `Alta` ou `Crítica`, qualquer origem;
2. origem **externa** ao assistente — usuário, homologação, auditoria, falha real de teste, de
   porta de qualidade ou de ambiente — em qualquer severidade;
3. pedido explícito de quem mantém o projeto.

Achado `Baixa`/`Média` que o próprio assistente encontrou revisando o próprio trabalho **não
entra**: é corrigido na hora ou descartado. **Nada corrigido dentro da mesma tarefa vira achado** —
o commit já é o rastro.

> Por que tão estreito: num projeto que rodou esse processo sem porta de entrada, o registro chegou
> a 515 achados com 81% já encerrados, e dos abertos **96 nasceram do próprio assistente auditando
> o próprio trabalho** contra 2 vindos do usuário, com só 7 de severidade alta. Registro que vira
> depósito de observação própria deixa de ser lido, e aí não protege de nada.

**Teto de 30 achados abertos.** Para o 31º entrar, um sai: encerrado de verdade, ou `Descartado`
com o motivo escrito no bloco. O `.githooks/pre-push` reprova o push que estoure o teto.

## Como registrar

1. **Pesquise antes de criar.** Procure aqui e **no acervo** por palavra-chave, módulo, rota,
   arquivo ou mensagem de erro. Pesquise **pelo caminho do arquivo** que vai no campo `Área`, não só
   por palavras do título — é a busca que alcança o achado escrito com outro vocabulário.
2. **Se já existe, atualize** a entrada em vez de criar outra.
3. **Defeito que volta depois de encerrado é achado novo**, nunca reabertura do bloco arquivado.
   Registre aqui citando o identificador arquivado — a citação é o que transforma "bug aleatório"
   em "recorrência", que é informação diferente.
4. ID no formato `ACH-YYYYMMDD-NNN`, com a data do registro e o próximo sequencial do dia.
5. Acrescente **uma linha no índice** e **um bloco em `## Achados`**.
6. Ao encerrar: mova a linha e o bloco para o acervo, com o estado final e a evidência de que
   fechou.

> Quando mais de uma frente trabalhar em paralelo em branches diferentes, o sequencial do dia colide
> na integração. A saída é sufixar a origem (`ACH-20260115-001-main`,
> `ACH-20260115-001-pagamento`), derivada da última palavra do slug da branch. Adote quando a
> colisão aparecer; até lá o formato simples basta.

### Modelo do bloco

```text
### ACH-YYYYMMDD-NNN — <resumo em uma linha>

- **Data:** YYYY-MM-DD
- **Origem:** usuário | homologação | teste | porta de qualidade | operação | revisão
- **Área:** caminho/do/Arquivo.java ou rota
- **Tipo:** bug | gap | inconsistência | risco operacional | documentação
- **Severidade:** Baixa | Média | Alta | Crítica
- **Reproduzir:** passos mínimos
- **Esperado:**
- **Observado:**
- **Evidência:** log, resposta HTTP, teste que falha
- **Risco:** segurança, dado pessoal, perda de dado, indisponibilidade
- **Decisão:**
- **Estado:** Novo | Em triagem | Precisa decisão | Aprovado para correção | Adiado
- **Links:** PR, commit, achado relacionado
```

## Estados

Abertos (ficam neste arquivo): `Novo`, `Em triagem`, `Precisa decisão`, `Aprovado para correção`,
`Adiado`.

Encerrados (vão para o acervo): `Corrigido`, `Validado`, `Risco aceito`, `Descartado`.

`Corrigido` é "o código mudou". `Validado` é "a correção foi provada no mesmo nível em que o
problema foi observado" — achado visto em homologação fecha com prova de homologação, não com teste
unitário. Ver [`../validacao/VALIDACAO.md`](../validacao/VALIDACAO.md).

## Índice

| ID | Data | Origem | Área | Tipo | Severidade | Resumo | Estado |
|---|---|---|---|---|---|---|---|
| ACH-20261009-001 | 2026-10-09 | porta de qualidade | `src/test/resources/application.properties` | gap | Alta | A suíte de testes não exercita o `application.properties` de produção: o arquivo de teste substitui o principal por completo, então divergência de configuração passa verde no `verify` e só aparece na aplicação de pé. | Precisa decisão |

## Achados

### ACH-20261009-001 — a suíte de testes não exercita o `application.properties` de produção

- **Data:** 2026-10-09
- **Origem:** porta de qualidade (`scripts/smoke.sh` da feature `rest`, contra a aplicação de pé)
- **Área:** `src/test/resources/application.properties`
- **Tipo:** gap
- **Severidade:** Alta
- **Reproduzir:** acrescente ao `src/main/resources/application.properties` qualquer propriedade
  que mude comportamento e **não** a replique no arquivo de teste; rode `./mvnw verify` (passa) e
  depois `scripts/smoke.sh` contra a aplicação de pé.
- **Esperado:** configuração que quebra o fluxo falha na suíte.
- **Observado:** passa na suíte e falha em runtime. Como o arquivo de teste tem o mesmo nome no
  classpath, ele **substitui** o principal — não o complementa. Nenhuma das 187 asserções enxerga
  o que o perfil real carrega.
- **Evidência:** `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect`
  estava fixado só no arquivo principal. `AuthFlowTest` provava que o token novo funciona depois da
  troca de senha e passava; na aplicação de pé **todo** token passava a tomar `401` depois da
  primeira troca de senha, porque o `passwordChangedAt` voltava do banco adiantado e o corte de
  `AuthService.isIssuedBeforeCutoff` rejeitava tudo. O dialect fixo foi removido (Hibernate já
  avisava `HHH90000025` que a propriedade é desnecessária), mas **o gap que escondeu o defeito
  continua aberto**.
- **Risco:** qualquer divergência futura entre os dois arquivos é invisível para o CI. Atinge
  autenticação, Flyway, fuso e segredo com default.
- **Decisão:** pendente. Saída proposta: apagar `src/test/resources/application.properties` e
  deixar os testes carregarem o `application.properties` real com o perfil `local`, sobrescrevendo
  **só** o datasource — que é o que o `support/CoreTestServer` já faz via `@DynamicPropertySource`.
  Custo a medir: o perfil `local` traz `db/migration-dev` e placeholders de seed, que a suíte hoje
  não espera.
- **Estado:** Precisa decisão
- **Links:** corrigido junto, no mesmo commit: remoção do dialect fixo em
  `src/main/resources/application.properties`
