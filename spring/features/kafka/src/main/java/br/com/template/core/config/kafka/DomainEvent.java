package br.com.template.core.config.kafka;

public record DomainEvent(String topic, String key, Object payload) {
}
