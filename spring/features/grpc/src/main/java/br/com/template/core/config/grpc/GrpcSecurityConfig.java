package br.com.template.core.config.grpc;

import br.com.template.core.config.security.jwt.services.JwtAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;

/**
 * Cadeia de segurança da porta gRPC — o equivalente do {@code SecurityConfig} para a porta HTTP.
 * Usa o mesmo {@link JwtAuthenticationProvider}, então a política de token (issuer, tipo, troca de
 * senha, sessão revogada) é literalmente a mesma nos dois caminhos.
 *
 * <p>Não toca no {@code SecurityConfig}: esta feature convive com a {@code rest} sem disputa.
 * Novos serviços caem em {@code allRequests().authenticated()}; libere ou restrinja por método aqui.
 */
@Configuration(proxyBeanMethods = false)
public class GrpcSecurityConfig {

    private static final String[] INFRA_METHODS = {
            "grpc.health.v1.Health/*",
            "grpc.reflection.v1.ServerReflection/*",
            "grpc.reflection.v1alpha.ServerReflection/*"
    };

    @Bean
    @GlobalServerInterceptor
    public AuthenticationProcessInterceptor grpcAuthenticationInterceptor(
            GrpcSecurity grpcSecurity, JwtAuthenticationProvider jwtAuthenticationProvider) throws Exception {
        return grpcSecurity
                .authenticationProvider(jwtAuthenticationProvider)
                .authenticationExtractor(new BearerJwtAuthenticationExtractor())
                .authorizeRequests(requests -> requests
                        .methods(INFRA_METHODS).permitAll()
                        .allRequests().authenticated())
                .build();
    }
}
