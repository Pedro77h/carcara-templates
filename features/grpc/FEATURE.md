# feature: grpc

Servidor gRPC numa porta própria (`9090`), autenticado com o **mesmo** `JwtAuthenticationProvider`
da porta HTTP, e um serviço de exemplo (`MeService/getMe`).

## O que entra

| Arquivo | Papel |
|---|---|
| `src/main/proto/user.proto` | contrato de exemplo: `MeService.getMe` |
| `config/grpc/GrpcSecurityConfig` | cadeia de segurança da porta gRPC — o `SecurityConfig` do gRPC |
| `config/grpc/BearerJwtAuthenticationExtractor` | lê o metadata `authorization: Bearer <token>` |
| `config/grpc/GrpcExceptionAdvice` | exceção de domínio → status gRPC (`EntityNotFoundException` → `NOT_FOUND`) |
| `modules/user/grpc/MeGrpcService` | exemplo de serviço; mesmo papel de um controller, fala só com components |
| `pom-snippet.xml` | `spring-boot-starter-grpc-server`, `-server-test` e o `protobuf-maven-plugin` |
| `properties-snippet.properties` | porta (`GRPC_PORT`, default `9090`) e reflection |
| `appends/docs/arquitetura/ARQUITETURA.md` | a seção "Porta gRPC" |

Não substitui nenhum arquivo do base. **Convive com a `rest`** sem disputa: o `SecurityConfig` é
da porta HTTP, o `GrpcSecurityConfig` é da porta gRPC, e os dois delegam a validação do token ao
`JwtAuthenticationProvider` do base. Logout, troca de senha e exclusão feitos pela API REST matam o
token na porta gRPC também — foi verificado ao vivo.

## Por que o provider fica no base

A validação do token não é só assinatura: confere `issuer`, tipo `access`, se o usuário ainda
existe, se o token é anterior à última troca de senha e se a sessão foi revogada. Duas cópias
dessa regra divergem na primeira mudança, e a porta esquecida vira a porta aberta. Por isso o
base expõe a política como `AuthenticationProvider` e cada porta só extrai o token.

## Emissão de token

Esta feature **não emite** token: valida. O token vem do `/api/auth/login` (feature `rest`) ou,
num serviço sem REST, de um serviço de auth externo que assine com o mesmo `app.jwt.secret` e
`app.jwt.issuer` — o desenho do Garçom, que delega auth ao `pdv.auth`. Sem `rest` e sem auth
externo, ninguém consegue token: escreva o RPC de login antes de expor o serviço.

## Como acrescentar um serviço

1. Declare o `service` em `src/main/proto/<modulo>.proto`, com
   `option java_package = "<pacote>.proto.<modulo>"`. O sufixo `.proto` importa: o JaCoCo do base
   exclui `**/proto/**`, que é código gerado.
2. Implemente em `modules/<modulo>/grpc/<Nome>GrpcService` com `@GrpcService`, estendendo o
   `*ImplBase` gerado. Injete **components**, nunca services ou repositories.
3. Novo método cai em `allRequests().authenticated()`. Para liberar ou exigir role, acrescente a
   regra no `GrpcSecurityConfig` com o nome completo `pacote.Servico/metodo`.
4. Exceção de negócio nova ganha `@GrpcExceptionHandler` no `GrpcExceptionAdvice`, senão o
   cliente recebe `UNKNOWN`.

Mudar um `.proto` publicado é mudança de API: acrescente campo com número novo; nunca reaproveite
nem renumere.

## Testes

`MeGrpcServiceTest` sobe o contexto inteiro com `@AutoConfigureTestGrpcTransport` (transporte em
processo, sem porta de rede) e prova: token válido devolve o perfil, sem token e lixo dão
`UNAUTHENTICATED`, e **sessão revogada dá `UNAUTHENTICATED`** — o caso que pegaria uma porta com
validação própria. `BearerJwtAuthenticationExtractorTest` cobre o parse do metadata.

## Validação ao vivo sem grpcurl

`curl` fala HTTP/2 e basta um frame vazio de 5 bytes para chamar um método sem argumento:

```bash
printf '\x00\x00\x00\x00\x00' > /tmp/empty.grpc
curl -s --http2-prior-knowledge -D - -o /dev/null -X POST \
  -H 'content-type: application/grpc' -H 'te: trailers' \
  -H "authorization: Bearer $TOKEN" \
  --data-binary @/tmp/empty.grpc \
  http://localhost:9090/br.com.template.core.user.MeService/getMe | grep grpc-status
```

`grpc-status: 0` é sucesso, `16` é `UNAUTHENTICATED`. Com `grpcurl` instalado, a reflection já
está ligada: `grpcurl -plaintext localhost:9090 list`.

## Atenção em produção

A reflection expõe o catálogo de serviços para quem alcançar a porta. Em produção, ou a porta
`9090` fica restrita à rede interna, ou desligue com `spring.grpc.server.reflection.enabled=false`
no `application-prod.properties`. O nginx de `deploy/` só faz proxy da porta HTTP.
