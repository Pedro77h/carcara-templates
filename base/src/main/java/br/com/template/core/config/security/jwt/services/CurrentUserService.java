package br.com.template.core.config.security.jwt.services;

import br.com.template.core.config.security.jwt.models.AuthenticatedUserPrincipal;
import br.com.template.core.modules.user.components.UserComponent;
import br.com.template.core.modules.user.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserComponent userComponent;

    public CurrentUserService(UserComponent userComponent) {
        this.userComponent = userComponent;
    }

    public User getCurrentUser() {
        return userComponent.findById(getCurrentUserId());
    }

    public Optional<Long> getCurrentUserIdOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication) || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return Optional.empty();
        }

        return Optional.of(principal.id());
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication) || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user not found");
        }

        return principal.id();
    }
}
