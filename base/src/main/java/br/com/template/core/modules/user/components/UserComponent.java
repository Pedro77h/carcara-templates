package br.com.template.core.modules.user.components;

import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class UserComponent {

    private final UserService userService;

    @Autowired
    public UserComponent(UserService userService) {
        this.userService = userService;
    }

    public User create(User user) {
        return this.userService.create(user);
    }

    public void revokeTokens(User user) {
        userService.revokeTokens(user);
    }

    public void registerLogin(User user) {
        userService.registerLogin(user);
    }

    public int deactivateInactiveSince(OffsetDateTime threshold) {
        return userService.deactivateInactiveSince(threshold);
    }

    public User update(User user) {
        return this.userService.update(user);
    }

    public int delete(Long userId) {
        return this.userService.delete(userId);
    }

    public User findById(Long id) {
        return this.userService.findById(id);
    }

    public User findByEmail(String email) {
        return this.userService.findByEmail(email);
    }

    public Optional<User> findByEmailOptional(String email) {
        return this.userService.findByEmailOptional(email);
    }

    public boolean existsByEmail(String email) {
        return this.userService.existsByEmail(email);
    }

    public void markEmailAsVerified(User user) {
        this.userService.markEmailAsVerified(user);
    }

    public void updatePassword(User user, String rawNewPassword) {
        this.userService.updatePassword(user, rawNewPassword);
    }

    public List<String> getRoleNames(User user) {
        return this.userService.getRoleNames(user);
    }

    public List<SimpleGrantedAuthority> getAuthorities(User user) {
        return this.userService.getAuthorities(user);
    }
}
