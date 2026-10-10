package br.com.template.core.core.services.storage;

/**
 * {@code contentType} e {@code fileName} vêm do cliente: servem para exibir, não para decidir
 * nada. Validar tipo de verdade é olhar o conteúdo.
 */
public record StoredFile(String key, String fileName, String contentType, long size) {
}
