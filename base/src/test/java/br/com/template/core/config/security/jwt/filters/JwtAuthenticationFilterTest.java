package br.com.template.core.config.security.jwt.filters;

import br.com.template.core.config.security.jwt.models.AuthenticatedUserPrincipal;
import br.com.template.core.config.security.jwt.models.JwtAuthenticationToken;
import br.com.template.core.config.security.jwt.services.JwtAuthenticationProvider;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "token-valido";

    @Mock
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtAuthenticationProvider);
        request = new MockHttpServletRequest("GET", "/api/user/me");
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should skip the public auth endpoints but not logout")
    void shouldSkipAuthEndpoints() {
        assertThat(jwtAuthenticationFilter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/auth/login"))).isTrue();
        assertThat(jwtAuthenticationFilter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/auth/logout"))).isFalse();
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("should authenticate the request when the provider accepts the bearer token")
    void shouldAuthenticate_whenBearerTokenValid() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(1L, "maria@example.com", List.of("ROLE_USER"));
        given(jwtAuthenticationProvider.authenticate(any())).willReturn(JwtAuthenticationToken.authenticated(
                TOKEN, principal, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("should continue anonymously when there is no token")
    void shouldContinueAnonymously_whenNoToken() throws Exception {
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        then(jwtAuthenticationProvider).should(never()).authenticate(any());
    }

    @Test
    @DisplayName("should ignore an access token offered through a cookie or an empty bearer")
    void shouldIgnoreTokenFromCookie() throws Exception {
        request.setCookies(new Cookie("access_token", TOKEN));
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer   ");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        then(jwtAuthenticationProvider).should(never()).authenticate(any());
    }

    @Test
    @DisplayName("should answer 401 and stop the chain when the provider refuses the token")
    void shouldAnswerUnauthorized_whenProviderRefuses() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        given(jwtAuthenticationProvider.authenticate(any())).willThrow(new BadCredentialsException("Invalid"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNull();
    }
}
