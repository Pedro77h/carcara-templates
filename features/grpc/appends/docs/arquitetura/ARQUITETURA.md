
## Porta gRPC (feature `grpc`)

O gRPC é uma **porta de entrada**, no mesmo nível dos controllers: `modules/<modulo>/grpc/` só fala
com components. A regra de direção das dependências vale igual.

```
config/grpc/
├── GrpcSecurityConfig                 autorização por método (o SecurityConfig da porta gRPC)
├── BearerJwtAuthenticationExtractor   metadata "authorization: Bearer" -> JwtAuthenticationToken
└── GrpcExceptionAdvice                exceção de domínio -> status gRPC
```

A autenticação usa o `JwtAuthenticationProvider` do base — a mesma política da porta HTTP. Nunca
valide token dentro de um `*GrpcService` nem num interceptor próprio.

Contratos ficam em `src/main/proto/`, com `java_package` terminando em `.proto.<modulo>` (código
gerado, fora da cobertura). Campo de `.proto` publicado nunca é renumerado nem reaproveitado.
