# Débito técnico

> O que foi deixado para depois **de propósito**, com o motivo e o caminho de saída. Atalho que
> não está aqui é atalho esquecido.

## Encaminhado

Nada ainda.

## Aberto e não encaminhado

### Dois serviços de token quase idênticos

`PasswordResetTokenService` e `EmailVerificationTokenService` repetem a mesma lógica de gerar,
hashear, invalidar pendentes, consumir e expurgar — diferem só na entidade, nas exceções e na
propriedade de validade. A parte realmente duplicada (gerar token aleatório + SHA-256) é pequena.

**Por que ficou assim:** são tabelas e ciclos de vida separados; unificar pede genérico sobre JPA,
que troca duplicação boba por abstração difícil de ler.

**Saída, se incomodar:** extrair só `generate()`/`hash()` para um utilitário em `core/utils` e
manter os dois services. Não vale criar `OneTimeTokenService<T>`.

### `JwtAuthenticationProvider` consulta o banco a cada requisição

O provider (usado pelo filtro HTTP e, com a feature `grpc`, pela porta gRPC) carrega o usuário por `id` em toda chamada autenticada, para conferir
`passwordChangedAt`/`sessionVersion` e as roles no banco (roles nunca são confiadas do cliente).

**Custo:** uma query por requisição autenticada.

**Saída, se virar gargalo:** cache curto por `userId` invalidado na troca de senha e no logout —
medir antes, a query é por chave primária.
