package br.com.template.core.modules.auth.services;

import br.com.template.core.config.security.jwt.services.JwtService;
import br.com.template.core.modules.auth.dtos.AuthResponse;
import br.com.template.core.modules.auth.exceptions.InvalidAuthenticationException;
import br.com.template.core.modules.email.EmailVerificationProperties;
import br.com.template.core.modules.email.PasswordResetProperties;
import br.com.template.core.modules.email.SmtpEmailService;
import br.com.template.core.modules.user.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordResetProperties passwordResetProperties;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final EmailVerificationProperties emailVerificationProperties;
    private final SmtpEmailService smtpEmailService;

    public AuthService(PasswordEncoder passwordEncoder, JwtService jwtService,
                       PasswordResetTokenService passwordResetTokenService,
                       PasswordResetProperties passwordResetProperties,
                       EmailVerificationTokenService emailVerificationTokenService,
                       EmailVerificationProperties emailVerificationProperties,
                       SmtpEmailService smtpEmailService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.passwordResetProperties = passwordResetProperties;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.emailVerificationProperties = emailVerificationProperties;
        this.smtpEmailService = smtpEmailService;
    }

    public AuthResponse buildAuthResponse(User user, List<String> roles) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .principalRole(resolvePrincipalRoleName(user))
                .roles(roles)
                .build();
    }

    public void deleteTokensByUserId(Long userId) {
        passwordResetTokenService.deleteAllByUserId(userId);
        emailVerificationTokenService.deleteAllByUserId(userId);
    }

    public void verifyPassword(String rawPassword, User user) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidAuthenticationException("Invalid credentials");
        }
    }

    public void spendPasswordVerificationTime(String rawPassword) {
        passwordEncoder.matches(rawPassword, DUMMY_PASSWORD_HASH);
    }

    public void sendEmailVerification(User user) {
        String token = emailVerificationTokenService.createTokenForUser(user);
        String link = emailVerificationProperties.verificationUrl() + "?token=" + token;

        try {
            smtpEmailService.sendEmailVerificationMail(user.getEmail(), link,
                    emailVerificationProperties.tokenExpirationMinutes());
        } catch (Exception ex) {
            log.error("Failed to send email verification for user {}", user.getId(), ex);
        }
    }

    public void sendRegistrationAttemptNotice(User user) {
        try {
            smtpEmailService.sendRegistrationAttemptMail(user.getEmail());
        } catch (Exception ex) {
            log.error("Failed to send registration attempt notice for user {}", user.getId(), ex);
        }
    }

    public User consumeEmailVerificationToken(String encodedToken) {
        return emailVerificationTokenService.validateAndConsume(encodedToken);
    }

    public void ensureRefreshAllowed(JwtService.TokenClaims tokenClaims, User user) {
        if (isIssuedBeforeCutoff(tokenClaims.issuedAt(), user.getPasswordChangedAt())) {
            throw new IllegalArgumentException("Token issued before password change");
        }

        if (isFromRevokedSession(tokenClaims, user)) {
            throw new IllegalArgumentException("Token issued before session revocation");
        }
    }

    public static boolean isIssuedBeforeCutoff(Instant issuedAt, OffsetDateTime cutoff) {
        return Objects.nonNull(cutoff) && issuedAt.isBefore(cutoff.toInstant().minusSeconds(1));
    }

    public static boolean isFromRevokedSession(JwtService.TokenClaims tokenClaims, User user) {
        return tokenClaims.sessionVersion() != user.getSessionVersion();
    }

    public void sendPasswordReset(User user) {
        String token = passwordResetTokenService.createTokenForUser(user);
        String link = buildResetPasswordUrl(token);

        try {
            smtpEmailService.sendRecoveryMail(user.getEmail(), link, passwordResetProperties.tokenExpirationMinutes());
        } catch (Exception ex) {
            log.error("Failed to send password reset email for user {}", user.getId(), ex);
        }
    }

    public User consumeResetToken(String encodedToken) {
        return passwordResetTokenService.validateAndConsume(encodedToken);
    }

    public void validateResetToken(String encodedToken) {
        passwordResetTokenService.validate(encodedToken);
    }

    private String buildResetPasswordUrl(String token) {
        return passwordResetProperties.resetUrl() + "?token=" + token;
    }

    private String resolvePrincipalRoleName(User user) {
        if (Objects.isNull(user.getPrincipalRole())) {
            return null;
        }
        return user.getPrincipalRole().name();
    }
}
