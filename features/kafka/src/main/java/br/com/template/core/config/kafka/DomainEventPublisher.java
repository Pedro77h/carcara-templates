package br.com.template.core.config.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(DomainEvent event) {
        kafkaTemplate.send(event.topic(), event.key(), event.payload())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish key {} to topic {}", event.key(), event.topic(), throwable);
                        return;
                    }

                    log.info("Published key {} to {}-{}", event.key(),
                            result.getRecordMetadata().topic(), result.getRecordMetadata().partition());
                });
    }
}
