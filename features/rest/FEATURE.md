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
| `docs/API.md` | **substitui** o do base, com todas as rotas documentadas |

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

## Atenção em produção

O rate limit conta por origem usando `request.getRemoteAddr()`. Atrás de proxy isso devolve o IP do
proxy para todo mundo, o que transforma o limite por origem num limite global. Os perfis `dev` e
`prod` já ligam `server.forward-headers-strategy=framework` e o nginx de `deploy/` já envia
`X-Forwarded-For` — mexer num sem o outro quebra o rate limit em silêncio. Ver `DEPLOY.md`.
