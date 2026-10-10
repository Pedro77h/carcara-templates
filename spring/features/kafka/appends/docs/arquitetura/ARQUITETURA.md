
## Publicação de evento (feature `kafka`)

Evento de domínio **nunca** é publicado no meio da transação. Quem publica é o
`DomainEventPublisher`, com `@TransactionalEventListener(phase = AFTER_COMMIT)`: se a transação der
rollback, o consumidor não recebe notícia de um fato que não existe no banco.

O service dispara o evento interno do Spring **dentro** da própria transação e esquece:

```java
@Transactional
public User register(User user) {
    User saved = userRepository.save(user);
    applicationEventPublisher.publishEvent(
            new DomainEvent("user.registered", saved.getId().toString(), userMapper.toUserDto(saved)));

    return saved;
}
```

A chave da mensagem (`key`) decide a partição: eventos com a mesma chave caem na mesma partição e
são consumidos **em ordem**. Use o id da entidade, não um aleatório — ordem por entidade é quase
sempre o que se quer.

O payload é serializado como JSON **sem header de tipo Java** (`addTypeInfo=false`). Isso é
deliberado: o consumidor desserializa no DTO dele, sem precisar de trusted packages nem conhecer
nossos pacotes. **O contrato é o JSON**, não a classe.

Mudar um payload já publicado é mudança de API: acrescente campo opcional, nunca renomeie nem
remova. Tópico não é criado pela aplicação — isso é trabalho da infraestrutura.
