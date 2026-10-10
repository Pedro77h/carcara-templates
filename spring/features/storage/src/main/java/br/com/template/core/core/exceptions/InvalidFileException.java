package br.com.template.core.core.exceptions;

/** Arquivo vazio, chave que escapa do diretório ou arquivo inexistente — erro do cliente, vira 400. */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
