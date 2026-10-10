package br.com.template.core.config.advices;

import br.com.template.core.modules.auth.exceptions.ExpiredEmailVerificationTokenException;
import br.com.template.core.modules.auth.exceptions.ExpiredResetTokenException;
import br.com.template.core.modules.auth.exceptions.InvalidAuthenticationException;
import br.com.template.core.modules.auth.exceptions.InvalidEmailVerificationTokenException;
import br.com.template.core.modules.auth.exceptions.InvalidResetTokenException;
import br.com.template.core.modules.auth.exceptions.TooManyAuthAttemptsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthAdvice {

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidAuthenticationException.class)
    public String handleInvalidAuthenticationException(InvalidAuthenticationException exception) {
        return exception.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidResetTokenException.class)
    public String handleInvalidResetTokenException(InvalidResetTokenException exception) {
        return exception.getMessage();
    }

    @ResponseStatus(HttpStatus.GONE)
    @ExceptionHandler(ExpiredResetTokenException.class)
    public String handleExpiredResetTokenException(ExpiredResetTokenException exception) {
        return exception.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidEmailVerificationTokenException.class)
    public String handleInvalidEmailVerificationTokenException(
            InvalidEmailVerificationTokenException exception) {
        return exception.getMessage();
    }

    @ResponseStatus(HttpStatus.GONE)
    @ExceptionHandler(ExpiredEmailVerificationTokenException.class)
    public String handleExpiredEmailVerificationTokenException(
            ExpiredEmailVerificationTokenException exception) {
        return exception.getMessage();
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(TooManyAuthAttemptsException.class)
    public String handleTooManyAuthAttemptsException(TooManyAuthAttemptsException exception) {
        return exception.getMessage();
    }
}
