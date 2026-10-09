package br.com.template.core.config.advices;

import br.com.template.core.modules.auth.exceptions.ExpiredEmailVerificationTokenException;
import br.com.template.core.modules.auth.exceptions.ExpiredResetTokenException;
import br.com.template.core.modules.auth.exceptions.InvalidAuthenticationException;
import br.com.template.core.modules.auth.exceptions.InvalidEmailVerificationTokenException;
import br.com.template.core.modules.auth.exceptions.InvalidResetTokenException;
import br.com.template.core.modules.auth.exceptions.TooManyAuthAttemptsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthAdviceTest {

    private final AuthAdvice authAdvice = new AuthAdvice();

    @Test
    @DisplayName("should return the InvalidAuthenticationException message")
    void shouldReturnInvalidAuthenticationMessage() {
        String result = authAdvice.handleInvalidAuthenticationException(
                new InvalidAuthenticationException("Invalid credentials"));

        assertThat(result).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("should return the InvalidResetTokenException message")
    void shouldReturnInvalidResetTokenMessage() {
        String result = authAdvice.handleInvalidResetTokenException(
                new InvalidResetTokenException("Invalid reset token"));

        assertThat(result).isEqualTo("Invalid reset token");
    }

    @Test
    @DisplayName("should return the ExpiredResetTokenException message")
    void shouldReturnExpiredResetTokenMessage() {
        String result = authAdvice.handleExpiredResetTokenException(
                new ExpiredResetTokenException("Expired reset token"));

        assertThat(result).isEqualTo("Expired reset token");
    }

    @Test
    @DisplayName("should return the InvalidEmailVerificationTokenException message")
    void shouldReturnInvalidEmailVerificationTokenMessage() {
        String result = authAdvice.handleInvalidEmailVerificationTokenException(
                new InvalidEmailVerificationTokenException("Invalid email verification token"));

        assertThat(result).isEqualTo("Invalid email verification token");
    }

    @Test
    @DisplayName("should return the ExpiredEmailVerificationTokenException message")
    void shouldReturnExpiredEmailVerificationTokenMessage() {
        String result = authAdvice.handleExpiredEmailVerificationTokenException(
                new ExpiredEmailVerificationTokenException("Expired email verification token"));

        assertThat(result).isEqualTo("Expired email verification token");
    }

    @Test
    @DisplayName("should return the TooManyAuthAttemptsException message")
    void shouldReturnTooManyAuthAttemptsMessage() {
        String result = authAdvice.handleTooManyAuthAttemptsException(
                new TooManyAuthAttemptsException("Too many attempts"));

        assertThat(result).isEqualTo("Too many attempts");
    }
}
