package br.com.template.core.modules.user.services;

import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(User user) {
        if (hasPassword(user)) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (Objects.isNull(user.getCreatedAt())) {
            user.setCreatedAt(OffsetDateTime.now());
        }
        return userRepository.save(user);
    }

    public void revokeTokens(User user) {
        user.setSessionVersion(user.getSessionVersion() + 1);
        userRepository.save(user);
    }

    public void registerLogin(User user) {
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public int deactivateInactiveSince(OffsetDateTime threshold) {
        return userRepository.deactivateInactiveSince(threshold, OffsetDateTime.now());
    }

    public User update(User user) {
        user.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public int delete(Long userId) {
        return userRepository.deleteAccountById(userId);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public Optional<User> findByEmailOptional(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    public void markEmailAsVerified(User user) {
        user.setEmailVerifiedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    public void updatePassword(User user, String rawNewPassword) {
        user.setPassword(passwordEncoder.encode(rawNewPassword));
        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    public List<String> getRoleNames(User user) {
        Set<String> roleNames = new LinkedHashSet<>();

        if (Objects.nonNull(user.getRoles())) {
            for (Role role : user.getRoles()) {
                roleNames.add(role.name());
            }
        }

        if (Objects.nonNull(user.getPrincipalRole())) {
            roleNames.add(user.getPrincipalRole().name());
        }

        return List.copyOf(roleNames);
    }

    public List<SimpleGrantedAuthority> getAuthorities(User user) {
        return getRoleNames(user).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private boolean hasPassword(User user) {
        return Objects.nonNull(user.getPassword()) && !user.getPassword().isBlank();
    }
}
