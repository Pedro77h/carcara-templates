package br.com.template.core.modules.auth.services;

import br.com.template.core.modules.auth.exceptions.ExpiredEmailVerificationTokenException;
import br.com.template.core.modules.auth.exceptions.InvalidEmailVerificationTokenException;
import br.com.template.core.modules.auth.models.EmailVerificationToken;
import br.com.template.core.modules.auth.repositories.EmailVerificationTokenRepository;
import br.com.template.core.modules.email.EmailVerificationProperties;
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
public class EmailVerificationTokenService {

    private static final int USED_TOKEN_RETENTION_HOURS = 24;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailVerificationProperties emailVerificationProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationTokenService(EmailVerificationTokenRepository emailVerificationTokenRepository,
                                         EmailVerificationProperties emailVerificationProperties) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailVerificationProperties = emailVerificationProperties;
    }

    @Transactional
    public String createTokenForUser(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        emailVerificationTokenRepository.invalidateAllPendingForUser(user.getId(), now);

        String encodedToken = buildEncodedToken();

        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .tokenHash(hash(encodedToken))
                .user(user)
                .expiresAt(now.plusMinutes(emailVerificationProperties.tokenExpirationMinutes()))
                .createdAt(now)
                .build());

        return encodedToken;
    }

    public User validateAndConsume(String encodedToken) {
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(hash(encodedToken))
                .orElseThrow(() -> new InvalidEmailVerificationTokenException("Invalid email verification token"));

        if (Objects.nonNull(token.getUsedAt())) {
            throw new InvalidEmailVerificationTokenException("Email verification token already used");
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ExpiredEmailVerificationTokenException("Expired email verification token");
        }

        token.setUsedAt(OffsetDateTime.now());
        emailVerificationTokenRepository.save(token);

        return token.getUser();
    }

    @Transactional
    public int deleteAllByUserId(Long userId) {
        return emailVerificationTokenRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public int purgeExhaustedTokens() {
        OffsetDateTime now = OffsetDateTime.now();

        return emailVerificationTokenRepository.deleteExhaustedTokens(now, now.minusHours(USED_TOKEN_RETENTION_HOURS));
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    private String buildEncodedToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
