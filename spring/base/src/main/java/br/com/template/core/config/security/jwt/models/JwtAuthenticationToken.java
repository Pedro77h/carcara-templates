package br.com.template.core.config.security.jwt.models;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String rawToken;
    private final AuthenticatedUserPrincipal principal;

    private JwtAuthenticationToken(String rawToken, AuthenticatedUserPrincipal principal,
                                   Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.rawToken = rawToken;
        this.principal = principal;
        setAuthenticated(Objects.nonNull(principal));
    }

    public static JwtAuthenticationToken unauthenticated(String rawToken) {
        return new JwtAuthenticationToken(rawToken, null, List.of());
    }

    public static JwtAuthenticationToken authenticated(String rawToken, AuthenticatedUserPrincipal principal,
                                                       Collection<? extends GrantedAuthority> authorities) {
        return new JwtAuthenticationToken(rawToken, principal, authorities);
    }

    @Override
    public Object getCredentials() {
        return rawToken;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public String getRawToken() {
        return rawToken;
    }
}
