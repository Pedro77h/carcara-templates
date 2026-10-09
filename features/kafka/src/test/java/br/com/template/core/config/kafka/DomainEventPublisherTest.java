package br.com.template.core.config.kafka;

import br.com.template.core.support.CoreTestServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@SpringBootTest
@Import(DomainEventPublisherTest.TransactionalSourceConfig.class)
class DomainEventPublisherTest extends CoreTestServer {

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TransactionalSource transactionalSource;

    static class TransactionalSource {

        private final ApplicationEventPublisher applicationEventPublisher;

        TransactionalSource(ApplicationEventPublisher applicationEventPublisher) {
            this.applicationEventPublisher = applicationEventPublisher;
        }

        @Transactional
        public void publishAndCommit(DomainEvent event) {
            applicationEventPublisher.publishEvent(event);
        }

        @Transactional
        public void publishAndRollback(DomainEvent event) {
            applicationEventPublisher.publishEvent(event);
            throw new IllegalStateException("rollback forçado");
        }
    }

    @TestConfiguration
    static class TransactionalSourceConfig {

        @Bean
        TransactionalSource transactionalSource(ApplicationEventPublisher applicationEventPublisher) {
            return new TransactionalSource(applicationEventPublisher);
        }
    }

    @Test
    @DisplayName("should publish to kafka only after the transaction commits")
    void shouldPublishAfterCommit() {
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(new CompletableFuture<>());

        transactionalSource.publishAndCommit(new DomainEvent("user.registered", "7", "carga"));

        then(kafkaTemplate).should().send("user.registered", "7", "carga");
    }

    @Test
    @DisplayName("should never publish to kafka when the transaction rolls back")
    void shouldNeverPublishOnRollback() {
        DomainEvent event = new DomainEvent("user.registered", "7", "carga");

        assertThatThrownBy(() -> transactionalSource.publishAndRollback(event))
                .isInstanceOf(IllegalStateException.class);

        then(kafkaTemplate).should(never()).send(anyString(), anyString(), any());
    }
}
