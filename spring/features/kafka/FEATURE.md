# feature: kafka

Produtor Kafka com publicação **após o commit**. Traz a infraestrutura e o padrão; não traz nenhum
evento de produto — o seu projeto declara os dele.

## O que entra

| Arquivo | Papel |
|---|---|
| `config/kafka/KafkaProducerConfig` | `ProducerFactory` + `KafkaTemplate`, JSON com o mesmo `JsonMapper` do HTTP |
| `config/kafka/DomainEvent` | `record (topic, key, payload)` — o evento interno do Spring |
| `config/kafka/DomainEventPublisher` | `@TransactionalEventListener(AFTER_COMMIT)` que manda para o Kafka |
| `pom-snippet.xml` | `spring-boot-starter-kafka` e `spring-boot-starter-kafka-test` |
| `properties-snippet.properties` | bootstrap, `acks=all`, idempotência, `delivery.timeout` |
| `appends/docs/arquitetura/ARQUITETURA.md` | a seção "Publicação de evento" |

Não substitui nenhum arquivo do base.

## Como usar

Injete o `ApplicationEventPublisher` do Spring no service e dispare o evento **dentro** da
transação. O `DomainEventPublisher` cuida do resto:

```java
@Transactional
public Order place(Order order) {
    Order saved = orderRepository.save(order);
    applicationEventPublisher.publishEvent(new DomainEvent("order.placed", saved.getId().toString(), dto));

    return saved;
}
```

Nada de injetar `KafkaTemplate` no service. O service não sabe que existe Kafka — ele anuncia um
fato, e quem resolve transporte é a camada de configuração. Trocar Kafka por outra coisa depois é
reescrever um bean, não N services.

## Três decisões que parecem detalhe e não são

**Publicar depois do commit.** `@TransactionalEventListener(AFTER_COMMIT)` em vez de mandar direto
no service. Rollback depois de um `send` deixa o consumidor com um fato que o banco não tem — e
desfazer isso do outro lado é bem mais caro que acertar aqui.

**`addTypeInfo=false`.** Sem o header com o nome da classe Java, o consumidor desserializa no DTO
dele sem precisar de trusted packages nem conhecer nossos pacotes. O contrato é o JSON. Com
`addTypeInfo=true` (o default do serializer) você acopla o consumidor ao seu nome de pacote, e
renomear uma classe interna quebra outro serviço.

**`acks=all` + idempotência.** `acks=all` espera a confirmação das réplicas;
`enable.idempotence=true` com `max.in.flight=5` evita duplicata na retentativa. Perder evento de
domínio em silêncio é pior que ficar 50ms mais lento.

## Tópico não é criado pela aplicação

De propósito: não há bean `NewTopic` aqui. Criação de tópico é trabalho da infraestrutura, onde
partição e replicação são decididas com o volume real na mão. Aplicação que cria tópico no startup
cria com o número errado de partições e ninguém descobre até doer.

Em desenvolvimento, `auto.create.topics.enable` do broker resolve.

## Testes

`DomainEventPublisherTest` prova as duas metades do contrato com o `KafkaTemplate` mockado: commit
publica, **rollback não publica**. Não precisa de broker no ar — e não deveria, porque o que pode
quebrar aqui é a ordem em relação à transação, não o Kafka.

Teste que precise de broker de verdade usa `@EmbeddedKafka` (vem no
`spring-boot-starter-kafka-test`). Vale para contrato de serialização; não vale para lógica.

## Rodando localmente

Acrescente ao `docker-compose.yml`:

```yaml
  kafka:
    image: apache/kafka:3.9.0
    container_name: core-kafka
    ports:
      - '9092:9092'
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

A aplicação sobe sem broker no ar: o produtor só tenta conectar no primeiro `send`.
