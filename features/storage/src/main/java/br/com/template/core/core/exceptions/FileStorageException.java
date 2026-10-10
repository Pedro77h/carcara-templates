package br.com.template.core.core.exceptions;

/** Falha de I/O do armazenamento — erro nosso, vira 500. */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
