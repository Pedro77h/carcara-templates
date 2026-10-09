package br.com.template.core.config.security.jwt.filters;

import br.com.template.core.config.security.jwt.models.AuthenticatedUserPrincipal;
import br.com.template.core.config.security.jwt.services.JwtService;
import br.com.template.core.modules.user.components.UserComponent;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "token-valido";

    @Mock
    private JwtService jwtService;
    @Mock
    private UserComponent userComponent;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userComponent);
        request = new MockHttpServletRequest("GET", "/api/user/me");
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("maria@example.com")
                .principalRole(Role.ROLE_USER)
                .roles(List.of(Role.ROLE_USER))
                .build();
    }

    private void givenTokenBelongsTo(User user) {
        given(jwtService.isTokenExpired(TOKEN)).willReturn(false);
        given(jwtService.parseAccessToken(TOKEN)).willReturn(
                new JwtService.TokenClaims(user.getId(), user.getEmail(), List.of("ROLE_USER"), "access", Instant.now(), 0));
        given(userComponent.findById(user.getId())).willReturn(user);
    }

    @Test
    @DisplayName("should skip the auth endpoints entirely")
    void shouldSkipAuthEndpoints() throws Exception {
        MockHttpServletRequest authRequest = new MockHttpServletRequest("POST", "/api/auth/login");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(authRequest)).isTrue();
    }

    @Test
    @DisplayName("should filter the protected endpoints")
    void shouldFilterProtectedEndpoints() {
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("should authenticate the request when the bearer token is valid")
    void shouldAuthenticate_whenBearerTokenValid() throws Exception {
        User user = buildUser();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        givenTokenBelongsTo(user);
        given(userComponent.getAuthorities(user)).willReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        given(userComponent.getRoleNames(user)).willReturn(List.of("ROLE_USER"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        AuthenticatedUserPrincipal principal =
                (AuthenticatedUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.id()).isEqualTo(1L);
        assertThat(principal.email()).isEqualTo("maria@example.com");
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("should continue anonymously when there is no token")
    void shouldContinueAnonymously_whenNoToken() throws Exception {
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("should ignore an access token offered through a cookie")
    void shouldIgnoreTokenFromCookie() throws Exception {
        request.setCookies(new Cookie("access_token", TOKEN));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("should answer 401 when the token is expired")
    void shouldAnswerUnauthorized_whenTokenExpired() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        given(jwtService.isTokenExpired(TOKEN)).willReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should answer 401 when the token is not an access token")
    void shouldAnswerUnauthorized_whenTokenTypeIsWrong() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        given(jwtService.isTokenExpired(TOKEN)).willReturn(false);
        given(jwtService.parseAccessToken(TOKEN)).willThrow(new JwtException("invalid"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("should answer 401 when the user of the token no longer exists")
    void shouldAnswerUnauthorized_whenUserNotFound() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        given(jwtService.isTokenExpired(TOKEN)).willReturn(false);
        given(jwtService.parseAccessToken(TOKEN)).willReturn(
                new JwtService.TokenClaims(9L, "sumiu@example.com", List.of(), "access", Instant.now(), 0));
        given(userComponent.findById(9L)).willThrow(new EntityNotFoundException("User not found"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("should answer 401 when the token was issued before the last password change")
    void shouldAnswerUnauthorized_whenTokenIssuedBeforePasswordChange() throws Exception {
        User user = buildUser();
        user.setPasswordChangedAt(OffsetDateTime.now());
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        given(jwtService.isTokenExpired(TOKEN)).willReturn(false);
        given(jwtService.parseAccessToken(TOKEN)).willReturn(
                new JwtService.TokenClaims(1L, user.getEmail(), List.of("ROLE_USER"), "access",
                        Instant.now().minusSeconds(600), 0));
        given(userComponent.findById(1L)).willReturn(user);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
