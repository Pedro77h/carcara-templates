package br.com.template.core.modules.auth.exceptions;

public class TooManyAuthAttemptsException extends RuntimeException {

    public TooManyAuthAttemptsException(String message) {
        super(message);
    }
}
