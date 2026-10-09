package br.com.template.core.modules.auth.services;

import br.com.template.core.modules.auth.exceptions.ExpiredResetTokenException;
import br.com.template.core.modules.auth.exceptions.InvalidResetTokenException;
import br.com.template.core.modules.auth.models.PasswordResetToken;
import br.com.template.core.modules.auth.repositories.PasswordResetTokenRepository;
import br.com.template.core.modules.auth.repositories.projections.PasswordResetTokenStatusProjection;
import br.com.template.core.modules.email.PasswordResetProperties;
import br.com.template.core.modules.user.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private PasswordResetTokenService passwordResetTokenService;

    @BeforeEach
    void setUp() {
        passwordResetTokenService = new PasswordResetTokenService(passwordResetTokenRepository,
                new PasswordResetProperties(30, "http://localhost:3000/reset-password"));
    }

    private User buildUser() {
        return User.builder().id(1L).email("maria@example.com").build();
    }

    private String hash(String rawToken) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes()));
    }

    private PasswordResetTokenStatusProjection buildStatus(OffsetDateTime usedAt, OffsetDateTime expiresAt) {
        return new PasswordResetTokenStatusProjection() {
            @Override
            public OffsetDateTime getUsedAt() {
                return usedAt;
            }

            @Override
            public OffsetDateTime getExpiresAt() {
                return expiresAt;
            }
        };
    }

    @Nested
    @DisplayName("createTokenForUser")
    class CreateTokenForUser {

        @Test
        @DisplayName("should invalidate pending tokens and store only the hash of the new token")
        void shouldInvalidatePendingAndStoreHash() throws NoSuchAlgorithmException {
            User user = buildUser();

            String encodedToken = passwordResetTokenService.createTokenForUser(user);

            then(passwordResetTokenRepository).should().invalidateAllPendingForUser(anyLong(), any(OffsetDateTime.class));
            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            then(passwordResetTokenRepository).should().save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).isEqualTo(hash(encodedToken));
            assertThat(captor.getValue().getUser()).isEqualTo(user);
            assertThat(captor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now());
        }

        @Test
        @DisplayName("should generate a different token on every call")
        void shouldGenerateDifferentTokens() {
            User user = buildUser();

            String firstToken = passwordResetTokenService.createTokenForUser(user);
            String secondToken = passwordResetTokenService.createTokenForUser(user);

            assertThat(firstToken).isNotEqualTo(secondToken);
        }
    }

    @Nested
    @DisplayName("validateAndConsume")
    class ValidateAndConsume {

        @Test
        @DisplayName("should mark the token as used and return its user")
        void shouldMarkTokenAsUsed() {
            User user = buildUser();
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .expiresAt(OffsetDateTime.now().plusMinutes(10))
                    .createdAt(OffsetDateTime.now())
                    .build();
            given(passwordResetTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(token));

            User result = passwordResetTokenService.validateAndConsume("token-gerado");

            assertThat(result).isEqualTo(user);
            assertThat(token.getUsedAt()).isNotNull();
            then(passwordResetTokenRepository).should().save(token);
        }

        @Test
        @DisplayName("should reject an unknown token")
        void shouldRejectUnknownToken() {
            given(passwordResetTokenRepository.findByTokenHash(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.validateAndConsume("token-desconhecido"))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        @DisplayName("should reject a token that was already used")
        void shouldRejectUsedToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(buildUser())
                    .usedAt(OffsetDateTime.now().minusMinutes(1))
                    .expiresAt(OffsetDateTime.now().plusMinutes(10))
                    .build();
            given(passwordResetTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetTokenService.validateAndConsume("token-gerado"))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        @DisplayName("should reject an expired token")
        void shouldRejectExpiredToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(buildUser())
                    .expiresAt(OffsetDateTime.now().minusMinutes(1))
                    .build();
            given(passwordResetTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetTokenService.validateAndConsume("token-gerado"))
                    .isInstanceOf(ExpiredResetTokenException.class);
            assertThat(token.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("should accept a pending token that has not expired")
        void shouldAcceptPendingToken() {
            given(passwordResetTokenRepository.findStatusByTokenHash(anyString()))
                    .willReturn(Optional.of(buildStatus(null, OffsetDateTime.now().plusMinutes(10))));

            passwordResetTokenService.validate("token-gerado");

            then(passwordResetTokenRepository).should().findStatusByTokenHash(anyString());
        }

        @Test
        @DisplayName("should reject an unknown token")
        void shouldRejectUnknownToken() {
            given(passwordResetTokenRepository.findStatusByTokenHash(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.validate("token-desconhecido"))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        @DisplayName("should reject a token that was already used")
        void shouldRejectUsedToken() {
            given(passwordResetTokenRepository.findStatusByTokenHash(anyString()))
                    .willReturn(Optional.of(buildStatus(OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(10))));

            assertThatThrownBy(() -> passwordResetTokenService.validate("token-gerado"))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        @DisplayName("should reject an expired token")
        void shouldRejectExpiredToken() {
            given(passwordResetTokenRepository.findStatusByTokenHash(anyString()))
                    .willReturn(Optional.of(buildStatus(null, OffsetDateTime.now().minusMinutes(1))));

            assertThatThrownBy(() -> passwordResetTokenService.validate("token-gerado"))
                    .isInstanceOf(ExpiredResetTokenException.class);
        }
    }

    @Nested
    @DisplayName("purgeExhaustedTokens")
    class PurgeExhaustedTokens {

        @Test
        @DisplayName("should delete expired unused tokens and used tokens older than the retention window")
        void shouldDeleteExhaustedTokens() {
            given(passwordResetTokenRepository.deleteExhaustedTokens(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(4);

            int result = passwordResetTokenService.purgeExhaustedTokens();

            assertThat(result).isEqualTo(4);
        }

        @Test
        @DisplayName("should keep used tokens inside the twenty four hour retention window")
        void shouldKeepUsedTokensInsideRetentionWindow() {
            ArgumentCaptor<OffsetDateTime> retentionLimitCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            given(passwordResetTokenRepository.deleteExhaustedTokens(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(0);

            passwordResetTokenService.purgeExhaustedTokens();

            then(passwordResetTokenRepository).should()
                    .deleteExhaustedTokens(any(OffsetDateTime.class), retentionLimitCaptor.capture());
            assertThat(retentionLimitCaptor.getValue())
                    .isCloseTo(OffsetDateTime.now().minusHours(24), within(1, java.time.temporal.ChronoUnit.MINUTES));
        }
    }

    @Test
    @DisplayName("should wipe every reset token of the user")
    void shouldWipeEveryTokenOfTheUser() {
        given(passwordResetTokenRepository.deleteAllByUserId(7L)).willReturn(2);

        assertThat(passwordResetTokenService.deleteAllByUserId(7L)).isEqualTo(2);
    }
}
