package br.com.template.core.config.advices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class NotFoundAdviceTest {

    private final NotFoundAdvice notFoundAdvice = new NotFoundAdvice();

    @Test
    @DisplayName("should handle EntityNotFoundException without throwing")
    void handleEntityNotFound_shouldNotThrow() {
        assertThatCode(notFoundAdvice::handleEntityNotFound).doesNotThrowAnyException();
    }
}
