package br.com.template.core.config.security.jwt.filters;

import br.com.template.core.config.security.jwt.models.AuthenticatedUserPrincipal;
import br.com.template.core.config.security.jwt.services.JwtService;
import br.com.template.core.modules.auth.services.AuthService;
import br.com.template.core.modules.user.components.UserComponent;
import br.com.template.core.modules.user.models.User;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PUBLIC_AUTH_PREFIX = "/api/auth/";
    private static final String LOGOUT_PATH = "/api/auth/logout";

    private final JwtService jwtService;
    private final UserComponent userComponent;

    public JwtAuthenticationFilter(JwtService jwtService, UserComponent userComponent) {
        this.jwtService = jwtService;
        this.userComponent = userComponent;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        return Objects.nonNull(requestUri)
                && requestUri.startsWith(PUBLIC_AUTH_PREFIX)
                && !LOGOUT_PATH.equals(requestUri);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (Objects.isNull(token) || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtService.isTokenExpired(token)) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return;
        }

        try {
            JwtService.TokenClaims tokenClaims = jwtService.parseAccessToken(token);
            User user = userComponent.findById(tokenClaims.userId());

            if (AuthService.isIssuedBeforeCutoff(tokenClaims.issuedAt(), user.getPasswordChangedAt())) {
                throw new IllegalArgumentException("Token issued before password change");
            }

            if (AuthService.isFromRevokedSession(tokenClaims, user)) {
                throw new IllegalArgumentException("Token issued before session revocation");
            }

            List<SimpleGrantedAuthority> authorities = userComponent.getAuthorities(user);

            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    userComponent.getRoleNames(user)
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (EntityNotFoundException | IllegalArgumentException | JwtException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (Objects.isNull(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
