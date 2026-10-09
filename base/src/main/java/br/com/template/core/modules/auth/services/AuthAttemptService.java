package br.com.template.core.modules.auth.services;

import br.com.template.core.core.components.PseudonymHasher;
import br.com.template.core.modules.auth.models.AuthAttempt;
import br.com.template.core.modules.auth.models.AuthAttemptScope;
import br.com.template.core.modules.auth.repositories.AuthAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthAttemptService {

    private final AuthAttemptRepository authAttemptRepository;
    private final PseudonymHasher pseudonymHasher;
    private final boolean enabled;
    private final int maxAttemptsPerEmail;
    private final int maxAttemptsPerOrigin;
    private final int blockMinutes;

    @Autowired
    public AuthAttemptService(AuthAttemptRepository authAttemptRepository,
                              PseudonymHasher pseudonymHasher,
                              @Value("${app.auth.rate-limit.enabled:true}") boolean enabled,
                              @Value("${app.auth.rate-limit.max-attempts:5}") int maxAttemptsPerEmail,
                              @Value("${app.auth.rate-limit.max-attempts-per-origin:20}") int maxAttemptsPerOrigin,
                              @Value("${app.auth.rate-limit.block-minutes:60}") int blockMinutes) {
        this.authAttemptRepository = authAttemptRepository;
        this.pseudonymHasher = pseudonymHasher;
        this.enabled = enabled;
        this.maxAttemptsPerEmail = maxAttemptsPerEmail;
        this.maxAttemptsPerOrigin = maxAttemptsPerOrigin;
        this.blockMinutes = blockMinutes;
    }

    public boolean isBlocked(AuthAttemptScope scope, String rawIdentifier) {
        if (!enabled) {
            return false;
        }

        return identifierOf(rawIdentifier)
                .map(identifier -> authAttemptRepository.countSince(scope, identifier, windowStart())
                        >= maxAttemptsFor(scope))
                .orElse(false);
    }

    private int maxAttemptsFor(AuthAttemptScope scope) {
        return AuthAttemptScope.LOGIN_ORIGIN.equals(scope) || AuthAttemptScope.PASSWORD_RESET_ORIGIN.equals(scope)
                ? maxAttemptsPerOrigin
                : maxAttemptsPerEmail;
    }

    @Transactional
    public void record(AuthAttemptScope scope, String rawIdentifier) {
        if (!enabled) {
            return;
        }

        identifierOf(rawIdentifier).ifPresent(identifier -> authAttemptRepository.save(AuthAttempt.builder()
                .scope(scope)
                .identifier(identifier)
                .attemptedAt(OffsetDateTime.now())
                .build()));
    }

    @Transactional
    public void clear(AuthAttemptScope scope, String rawIdentifier) {
        identifierOf(rawIdentifier)
                .ifPresent(identifier -> authAttemptRepository.deleteByScopeAndIdentifier(scope, identifier));
    }

    @Transactional
    public int purgeAttemptedBefore(OffsetDateTime threshold) {
        return authAttemptRepository.deleteAttemptedBefore(threshold);
    }

    private Optional<String> identifierOf(String rawIdentifier) {
        if (Objects.isNull(rawIdentifier) || rawIdentifier.isBlank()) {
            return Optional.empty();
        }

        return pseudonymHasher.hash(rawIdentifier.strip().toLowerCase(Locale.ROOT));
    }

    private OffsetDateTime windowStart() {
        return OffsetDateTime.now().minusMinutes(blockMinutes);
    }
}
