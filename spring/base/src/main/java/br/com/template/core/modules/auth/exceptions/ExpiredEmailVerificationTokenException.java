package br.com.template.core.modules.auth.exceptions;

public class ExpiredEmailVerificationTokenException extends RuntimeException {
    public ExpiredEmailVerificationTokenException(String message) {
        super(message);
    }
}
