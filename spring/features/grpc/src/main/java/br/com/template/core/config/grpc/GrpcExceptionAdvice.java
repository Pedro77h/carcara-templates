package br.com.template.core.config.grpc;

import io.grpc.Status;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.grpc.server.advice.GrpcExceptionHandler;

/**
 * Traduz exceção de domínio em status gRPC, para o cliente receber erro classificado em vez de
 * {@code UNKNOWN}. Autenticação e autorização já viram {@code UNAUTHENTICATED}/{@code PERMISSION_DENIED}
 * pelo handler de segurança do Spring gRPC.
 */
@GrpcAdvice
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler
    public Status handleNotFound(EntityNotFoundException exception) {
        return Status.NOT_FOUND.withDescription(exception.getMessage());
    }
}
