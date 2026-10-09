package br.com.template.core.core.components;

import br.com.template.core.config.security.jwt.services.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUserComponent {

    private final CurrentUserService currentUserService;

    @Autowired
    public CurrentUserComponent(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public Long getCurrentUserId() {
        return currentUserService.getCurrentUserId();
    }

    public Optional<Long> findCurrentUserId() {
        return currentUserService.getCurrentUserIdOptional();
    }
}
