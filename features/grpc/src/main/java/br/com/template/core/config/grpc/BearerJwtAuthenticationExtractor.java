package br.com.template.core.config.grpc;

import br.com.template.core.config.security.jwt.models.JwtAuthenticationToken;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.springframework.grpc.server.security.GrpcAuthenticationExtractor;
import org.springframework.security.core.Authentication;

import java.util.Locale;
import java.util.Objects;

/**
 * Lê o metadata {@code authorization: Bearer <token>} — o equivalente gRPC do header que o
 * {@code JwtAuthenticationFilter} lê na porta HTTP. Sem token, devolve {@code null} e a chamada
 * segue anônima até a regra de autorização decidir.
 */
public class BearerJwtAuthenticationExtractor implements GrpcAuthenticationExtractor {

    private static final String BEARER_PREFIX = "bearer ";

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public Authentication extract(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method) {
        String authorization = headers.get(AUTHORIZATION_KEY);

        if (Objects.isNull(authorization) || !authorization.toLowerCase(Locale.ROOT).startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();

        return token.isEmpty() ? null : JwtAuthenticationToken.unauthenticated(token);
    }
}
