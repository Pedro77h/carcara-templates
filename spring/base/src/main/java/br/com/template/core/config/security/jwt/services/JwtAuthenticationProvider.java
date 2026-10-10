package br.com.template.core.config.security.jwt.services;

import br.com.template.core.config.security.jwt.models.AuthenticatedUserPrincipal;
import br.com.template.core.config.security.jwt.models.JwtAuthenticationToken;
import br.com.template.core.modules.auth.services.AuthService;
import br.com.template.core.modules.user.components.UserComponent;
import br.com.template.core.modules.user.models.User;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtService jwtService;
    private final UserComponent userComponent;

    public JwtAuthenticationProvider(JwtService jwtService, UserComponent userComponent) {
        this.jwtService = jwtService;
        this.userComponent = userComponent;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        JwtAuthenticationToken request = (JwtAuthenticationToken) authentication;

        try {
            JwtService.TokenClaims tokenClaims = jwtService.parseAccessToken(request.getRawToken());
            User user = userComponent.findById(tokenClaims.userId());

            if (AuthService.isIssuedBeforeCutoff(tokenClaims.issuedAt(), user.getPasswordChangedAt())) {
                throw new BadCredentialsException("Token issued before password change");
            }

            if (AuthService.isFromRevokedSession(tokenClaims, user)) {
                throw new BadCredentialsException("Token issued before session revocation");
            }

            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                    user.getId(), user.getEmail(), userComponent.getRoleNames(user));

            return JwtAuthenticationToken.authenticated(
                    request.getRawToken(), principal, userComponent.getAuthorities(user));
        } catch (EntityNotFoundException | IllegalArgumentException | JwtException exception) {
            throw new BadCredentialsException("Invalid or expired token", exception);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
