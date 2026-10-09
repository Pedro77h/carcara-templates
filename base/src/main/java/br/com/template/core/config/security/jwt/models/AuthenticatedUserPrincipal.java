package br.com.template.core.config.security.jwt.models;

import java.util.List;

public record AuthenticatedUserPrincipal(
        Long id,
        String email,
        List<String> roles
) {
}
