package br.com.template.core.modules.auth.services;

import br.com.template.core.modules.auth.exceptions.ExpiredResetTokenException;
import br.com.template.core.modules.auth.exceptions.InvalidResetTokenException;
import br.com.template.core.modules.auth.models.PasswordResetToken;
import br.com.template.core.modules.auth.repositories.PasswordResetTokenRepository;
import br.com.template.core.modules.auth.repositories.projections.PasswordResetTokenStatusProjection;
import br.com.template.core.modules.email.PasswordResetProperties;
import br.com.template.core.modules.user.models.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class PasswordResetTokenService {

    private static final int USED_TOKEN_RETENTION_HOURS = 24;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetProperties passwordResetProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository,
                                     PasswordResetProperties passwordResetProperties) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetProperties = passwordResetProperties;
    }

    @Transactional
    public int deleteAllByUserId(Long userId) {
        return passwordResetTokenRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public int purgeExhaustedTokens() {
        OffsetDateTime now = OffsetDateTime.now();

        return passwordResetTokenRepository.deleteExhaustedTokens(now, now.minusHours(USED_TOKEN_RETENTION_HOURS));
    }

    @Transactional
    public String createTokenForUser(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        passwordResetTokenRepository.invalidateAllPendingForUser(user.getId(), now);

        String encodedToken = this.buildEncodedResetToken();

        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(hash(encodedToken))
                .user(user)
                .expiresAt(now.plusMinutes(passwordResetProperties.tokenExpirationMinutes()))
                .createdAt(now)
                .build();

        passwordResetTokenRepository.save(token);

        return encodedToken;
    }

    public User validateAndConsume(String encodedToken) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(encodedToken))
                .orElseThrow(() -> new InvalidResetTokenException("Invalid reset token"));

        assertUsable(token.getUsedAt(), token.getExpiresAt());

        token.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepository.save(token);

        return token.getUser();
    }

    public void validate(String encodedToken) {
        PasswordResetTokenStatusProjection status = passwordResetTokenRepository.findStatusByTokenHash(hash(encodedToken))
                .orElseThrow(() -> new InvalidResetTokenException("Invalid reset token"));

        assertUsable(status.getUsedAt(), status.getExpiresAt());
    }

    private void assertUsable(OffsetDateTime usedAt, OffsetDateTime expiresAt) {
        if (Objects.nonNull(usedAt)) {
            throw new InvalidResetTokenException("Reset token already used");
        }
        if (expiresAt.isBefore(OffsetDateTime.now())) {
            throw new ExpiredResetTokenException("Expired reset token");
        }
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes());
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    private String buildEncodedResetToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
