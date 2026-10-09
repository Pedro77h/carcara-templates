# feature: rest

Camada REST sobre o `auth`/`user` do base: os controllers, o advice que traduz as exceções de
autenticação em status HTTP, e o `SecurityConfig` com as rotas públicas de `/api/auth/**`.

Sem esta feature o projeto sobe, autentica e tem toda a lógica de conta — só não tem por onde
chamar, além do `/api/ping`. Escolha-a para qualquer projeto cuja porta de entrada seja HTTP.

## O que entra

| Arquivo | Papel |
|---|---|
| `modules/auth/controller/AuthController` | register, verify-email, login, refresh, logout, forgot/reset de senha |
| `modules/user/controller/MeController` | `GET`/`PATCH`/`DELETE /api/me` e `POST /api/me/password` |
| `config/advices/AuthAdvice` | exceção de auth → `401`/`400`/`410`/`429` |
| `scripts/smoke.sh` | smoke e2e do fluxo de conta contra uma instância de pé |
| `docs/arquitetura/API.md` | **substitui** o do base, com todas as rotas documentadas |
| `appends/docs/validacao/VALIDACAO.md` | acrescenta a seção do smoke ao `docs/validacao/VALIDACAO.md` do base |

## O que substitui

`config/security/SecurityConfig.java`. A versão do base só libera `/api/ping`, Swagger e actuator;
esta libera também `/api/auth/**`, **exceto** `POST /api/auth/logout`, que exige token.

É substituição de arquivo inteiro, de propósito: mesclar regras de autorização dentro de um
`authorizeHttpRequests` por script é frágil e silencioso quando erra. Ao adicionar rotas públicas
no seu projeto, edite este arquivo direto.

## Dependências

Nenhuma nova — `spring-boot-starter-webmvc`, `validation` e `springdoc` já estão no base, porque
Swagger e actuator precisam deles de qualquer forma.

## Testes

`AuthControllerTest`, `MeControllerTest` e `AuthAdviceTest` (unitários); `SecurityConfigTest`
(contexto real, confere que CSRF não barra, que `logout` exige token e que `/api/me` exige token);
`AuthFlowTest` e `AuthRateLimitFlowTest` (fluxo ponta a ponta com H2 + Flyway de verdade).

`AuthFlowTest` cobre o que vale checar num esqueleto de auth: cadastro não loga antes de confirmar
o e-mail, e-mail repetido não cria segunda conta nem muda a resposta, troca de senha **mata o token
antigo**, exclusão invalida o token e apaga a linha, logout derruba a sessão, e `forgot-password`
responde igual para e-mail conhecido e desconhecido.

`AuthRateLimitFlowTest` confere o `429` após o limite, a limpeza do contador no login bem-sucedido
e que o identificador gravado em `auth_attempts` **não** contém o e-mail nem o IP em claro.

### E o smoke, que pega o que nenhum deles pega

`scripts/smoke.sh` percorre o mesmo fluxo por HTTP real contra a aplicação de pé. Não é redundância
com o `AuthFlowTest`: os testes substituem o `application.properties` por um de teste, então
**divergência de configuração passa verde na suíte**. Foi o smoke que achou o `hibernate.dialect`
fixado no arquivo principal, que fazia todo token tomar `401` depois da primeira troca de senha
enquanto o `AuthFlowTest` jurava que funcionava. Ver `ACH-20261009-001` em
`docs/achados/REGISTRO_ACHADOS.md`.

## O cadastro responde 202 sempre, e isso é contrato

`POST /api/auth/register` devolve **`202` sem corpo em todos os casos**: e-mail livre ou e-mail já
cadastrado, verificação de e-mail ligada ou desligada. Nunca devolve token.

Não é descuido de modelagem. Status diferente por caso transforma o cadastro em oráculo de
enumeração: quem recebe `200` sabe que o e-mail estava livre, quem recebe `202` sabe que já tem
conta. Como o par de tokens só vem do `/login`, a resposta do cadastro não carrega informação
nenhuma sobre a existência da conta — que é o objetivo.

Em modo de demonstração (`app.auth.email-verification-required=false`) a conta nasce confirmada e o
`/login` seguinte funciona de imediato: uma chamada a mais, zero vazamento.

## Atenção em produção

O rate limit conta por origem usando `request.getRemoteAddr()`. Atrás de proxy isso devolve o IP do
proxy para todo mundo, o que transforma o limite por origem num limite global. Os perfis `dev` e
`prod` já ligam `server.forward-headers-strategy=framework` e o nginx de `deploy/` já envia
`X-Forwarded-For` — mexer num sem o outro quebra o rate limit em silêncio. Ver `DEPLOY.md`.
