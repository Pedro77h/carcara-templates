package br.com.template.core.config.grpc;

import br.com.template.core.config.security.jwt.models.JwtAuthenticationToken;
import io.grpc.Attributes;
import io.grpc.Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class BearerJwtAuthenticationExtractorTest {

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final BearerJwtAuthenticationExtractor extractor = new BearerJwtAuthenticationExtractor();

    private Authentication extract(String authorization) {
        Metadata headers = new Metadata();
        if (authorization != null) {
            headers.put(AUTHORIZATION, authorization);
        }

        return extractor.extract(headers, Attributes.EMPTY, null);
    }

    @Test
    @DisplayName("should extract the token from a bearer header regardless of case")
    void shouldExtractBearerToken() {
        assertThat(extract("Bearer abc")).isInstanceOfSatisfying(JwtAuthenticationToken.class,
                token -> assertThat(token.getRawToken()).isEqualTo("abc"));
        assertThat(extract("bearer  abc ")).isInstanceOfSatisfying(JwtAuthenticationToken.class,
                token -> assertThat(token.getRawToken()).isEqualTo("abc"));
    }

    @Test
    @DisplayName("should leave the call anonymous when the header is missing, empty or not bearer")
    void shouldReturnNull_whenNoBearer() {
        assertThat(extract(null)).isNull();
        assertThat(extract("Bearer   ")).isNull();
        assertThat(extract("Basic dXNlcjpwYXNz")).isNull();
    }
}
