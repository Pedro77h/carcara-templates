package br.com.template.core.config.security.jwt.services;

import br.com.template.core.config.security.jwt.models.JwtProperties;
import io.jsonwebtoken.JwtException;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "xrfUp0mab+ikm0j9LJYtbhzTog8hiYlkVuqb039kVCYu5Dlbmfx5ZMHRg6wcm7Y8";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, "core", 60, 7));
        user = User.builder()
                .id(42L)
                .email("maria@example.com")
                .principalRole(Role.ROLE_USER)
                .roles(List.of(Role.ROLE_USER, Role.ROLE_ADMIN))
                .build();
    }

    @Nested
    @DisplayName("token generation")
    class TokenGeneration {

        @Test
        @DisplayName("should generate an access token with the expected claims")
        void shouldGenerateAccessTokenWithExpectedClaims() {
            String token = jwtService.generateAccessToken(user);

            JwtService.TokenClaims claims = jwtService.parseAccessToken(token);

            assertThat(claims.userId()).isEqualTo(42L);
            assertThat(claims.subject()).isEqualTo("maria@example.com");
            assertThat(claims.tokenType()).isEqualTo("access");
            assertThat(claims.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
            assertThat(claims.issuedAt()).isNotNull();
        }

        @Test
        @DisplayName("should generate a refresh token with the expected claims")
        void shouldGenerateRefreshTokenWithExpectedClaims() {
            String token = jwtService.generateRefreshToken(user);

            JwtService.TokenClaims claims = jwtService.parseRefreshToken(token);

            assertThat(claims.userId()).isEqualTo(42L);
            assertThat(claims.tokenType()).isEqualTo("refresh");
        }

        @Test
        @DisplayName("should not duplicate the principal role in the roles claim")
        void shouldNotDuplicatePrincipalRole() {
            User singleRoleUser = User.builder()
                    .id(7L)
                    .email("ana@example.com")
                    .principalRole(Role.ROLE_USER)
                    .roles(List.of(Role.ROLE_USER))
                    .build();

            JwtService.TokenClaims claims = jwtService.parseAccessToken(jwtService.generateAccessToken(singleRoleUser));

            assertThat(claims.roles()).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should generate a token when the user has no roles list")
        void shouldGenerateTokenWhenRolesAreNull() {
            User roleLessUser = User.builder().id(9L).email("sem-role@example.com").build();

            JwtService.TokenClaims claims = jwtService.parseAccessToken(jwtService.generateAccessToken(roleLessUser));

            assertThat(claims.roles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("token parsing")
    class TokenParsing {

        @Test
        @DisplayName("should reject a refresh token when an access token is expected")
        void shouldRejectRefreshTokenAsAccessToken() {
            String refreshToken = jwtService.generateRefreshToken(user);

            assertThatThrownBy(() -> jwtService.parseAccessToken(refreshToken))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject an access token when a refresh token is expected")
        void shouldRejectAccessTokenAsRefreshToken() {
            String accessToken = jwtService.generateAccessToken(user);

            assertThatThrownBy(() -> jwtService.parseRefreshToken(accessToken))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should refuse a malformed token")
        void shouldRefuseMalformedToken() {
            assertThatThrownBy(() -> jwtService.parseAccessToken("nao-e-um-token"))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("should refuse an already expired token")
        void shouldRefuseExpiredToken() {
            JwtService expiredJwtService = new JwtService(new JwtProperties(SECRET, "core", -1, -1));
            String token = expiredJwtService.generateAccessToken(user);

            assertThatThrownBy(() -> expiredJwtService.parseAccessToken(token))
                    .isInstanceOf(JwtException.class);
        }
    }
}
