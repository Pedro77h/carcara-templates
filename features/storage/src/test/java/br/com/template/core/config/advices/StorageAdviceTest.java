package br.com.template.core.config.advices;

import br.com.template.core.core.exceptions.InvalidFileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageAdviceTest {

    private final StorageAdvice storageAdvice = new StorageAdvice();

    @Test
    @DisplayName("should expose the reason of an invalid file and a fixed message for oversized uploads")
    void shouldTranslateStorageErrors() {
        assertThat(storageAdvice.handleInvalidFile(new InvalidFileException("Cannot store an empty file")))
                .isEqualTo("Cannot store an empty file");
        assertThat(storageAdvice.handleTooLarge()).isEqualTo("File exceeds the maximum upload size");
    }
}
