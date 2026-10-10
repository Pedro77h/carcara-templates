# Registro de achados — acervo

Histórico dos achados **encerrados**. Imutável: bloco que chega aqui não é editado nem reaberto.

Achado aberto vive em [`REGISTRO_ACHADOS.md`](REGISTRO_ACHADOS.md). Ao encerrar um achado, mova a
linha de índice e o bloco para cá, com o estado final (`Corrigido`, `Validado`, `Risco aceito`,
`Descartado`) e a evidência de que fechou.

**Defeito que volta não reabre bloco daqui.** Ele é um achado novo no registro vivo, citando o
identificador arquivado — é essa citação que o classifica como recorrência, que é informação
diferente de um bug inédito e pede tratamento diferente (causa raiz, não remendo).

Pesquise aqui antes de registrar achado novo: metade do trabalho de triagem é descobrir que o
problema já foi visto.

## Índice

| ID | Data | Origem | Área | Tipo | Severidade | Resumo | Estado | Encerrado em |
|---|---|---|---|---|---|---|---|---|
| ACH-20261009-001 | 2026-10-09 | porta de qualidade | `src/test/resources` | gap | Alta | Suíte não exercitava o `application.properties` de produção | Corrigido | 2026-10-09 |

## Achados encerrados

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
- **Decisão:** 2026-10-09 — o arquivo de teste saiu da raiz e foi para
  `src/test/resources/config/application.properties`, que o Spring Boot carrega **por cima** do
  principal em vez de no lugar dele. Só traz o que difere em teste (H2, `migration-test`, segredo,
  URLs que só existem nos perfis) e ativa o perfil `test`, para o default `local` não puxar
  `migration-dev` nem o seed. Funciona também rodando o teste pela IDE, sem flag de Maven.
- **Estado:** Corrigido
- **Validação:** `CoreApplicationTests.shouldLoadMainPropertiesUnderTestOverrides` falha se o
  principal deixar de chegar aos testes ou se o perfil `local` voltar a ser ativado. A feature
  `storage` perdeu o default de contorno no `@Value` e passa na matriz lendo o snippet real.
- **Links:** corrigido junto, no mesmo commit: remoção do dialect fixo em
  `src/main/resources/application.properties`

