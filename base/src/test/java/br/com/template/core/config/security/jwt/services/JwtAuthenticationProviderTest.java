package br.com.template.core.config.security.jwt.services;

import br.com.template.core.config.security.jwt.models.AuthenticatedUserPrincipal;
import br.com.template.core.config.security.jwt.models.JwtAuthenticationToken;
import br.com.template.core.modules.user.components.UserComponent;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationProviderTest {

    private static final String TOKEN = "token-valido";

    @Mock
    private JwtService jwtService;
    @Mock
    private UserComponent userComponent;

    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @BeforeEach
    void setUp() {
        jwtAuthenticationProvider = new JwtAuthenticationProvider(jwtService, userComponent);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("maria@example.com")
                .principalRole(Role.ROLE_USER)
                .roles(List.of(Role.ROLE_USER))
                .build();
    }

    private void givenClaims(long userId, Instant issuedAt, int sessionVersion) {
        given(jwtService.parseAccessToken(TOKEN)).willReturn(new JwtService.TokenClaims(
                userId, "maria@example.com", List.of("ROLE_USER"), "access", issuedAt, sessionVersion));
    }

    private Authentication authenticate() {
        return jwtAuthenticationProvider.authenticate(JwtAuthenticationToken.unauthenticated(TOKEN));
    }

    @Test
    @DisplayName("should build the principal and authorities from the current state of the user")
    void shouldAuthenticate_whenTokenValid() {
        User user = buildUser();
        givenClaims(1L, Instant.now(), 0);
        given(userComponent.findById(1L)).willReturn(user);
        given(userComponent.getRoleNames(user)).willReturn(List.of("ROLE_USER"));
        given(userComponent.getAuthorities(user)).willReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        Authentication authentication = authenticate();

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUserPrincipal(1L, "maria@example.com", List.of("ROLE_USER")));
        assertThat(authentication.getCredentials()).isEqualTo(TOKEN);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("should refuse an expired, malformed or non-access token")
    void shouldRefuse_whenTokenInvalid() {
        given(jwtService.parseAccessToken(TOKEN)).willThrow(new JwtException("invalid"));

        assertThatThrownBy(this::authenticate).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("should refuse when the user of the token no longer exists")
    void shouldRefuse_whenUserNotFound() {
        givenClaims(9L, Instant.now(), 0);
        given(userComponent.findById(9L)).willThrow(new EntityNotFoundException("User not found"));

        assertThatThrownBy(this::authenticate).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("should refuse a token issued before the last password change")
    void shouldRefuse_whenIssuedBeforePasswordChange() {
        User user = buildUser();
        user.setPasswordChangedAt(OffsetDateTime.now());
        givenClaims(1L, Instant.now().minusSeconds(600), 0);
        given(userComponent.findById(1L)).willReturn(user);

        assertThatThrownBy(this::authenticate).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("should refuse a token from a revoked session")
    void shouldRefuse_whenSessionRevoked() {
        User user = buildUser();
        user.setSessionVersion(1);
        givenClaims(1L, Instant.now(), 0);
        given(userComponent.findById(1L)).willReturn(user);

        assertThatThrownBy(this::authenticate).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("should only support jwt authentication tokens")
    void shouldSupportOnlyJwtTokens() {
        assertThat(jwtAuthenticationProvider.supports(JwtAuthenticationToken.class)).isTrue();
        assertThat(jwtAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
    }
}
