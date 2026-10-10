package br.com.template.core.modules.auth.services;

import br.com.template.core.modules.auth.exceptions.ExpiredEmailVerificationTokenException;
import br.com.template.core.modules.auth.exceptions.InvalidEmailVerificationTokenException;
import br.com.template.core.modules.auth.models.EmailVerificationToken;
import br.com.template.core.modules.auth.repositories.EmailVerificationTokenRepository;
import br.com.template.core.modules.email.EmailVerificationProperties;
import br.com.template.core.modules.user.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenServiceTest {

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    private EmailVerificationTokenService emailVerificationTokenService;

    @BeforeEach
    void setUp() {
        emailVerificationTokenService = new EmailVerificationTokenService(emailVerificationTokenRepository,
                new EmailVerificationProperties(1440, "http://localhost:3000/verificar-email"));
    }

    private User buildUser() {
        return User.builder().id(1L).email("maria@example.com").build();
    }

    @Nested
    @DisplayName("createTokenForUser")
    class CreateTokenForUser {

        @Test
        @DisplayName("should invalidate the pending tokens and store only the hash of the new one")
        void shouldStoreOnlyTheHash() {
            ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
            User user = buildUser();

            String encodedToken = emailVerificationTokenService.createTokenForUser(user);

            then(emailVerificationTokenRepository).should().invalidateAllPendingForUser(eq(1L), any(OffsetDateTime.class));
            then(emailVerificationTokenRepository).should().save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).isNotEqualTo(encodedToken);
            assertThat(captor.getValue().getUser()).isEqualTo(user);
            assertThat(captor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now());
        }
    }

    @Nested
    @DisplayName("validateAndConsume")
    class ValidateAndConsume {

        private EmailVerificationToken buildToken() {
            return EmailVerificationToken.builder()
                    .user(buildUser())
                    .expiresAt(OffsetDateTime.now().plusHours(1))
                    .createdAt(OffsetDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("should return the user and mark the token as used")
        void shouldConsumeToken() {
            EmailVerificationToken token = buildToken();
            given(emailVerificationTokenRepository.findByTokenHash(any(String.class))).willReturn(Optional.of(token));

            User result = emailVerificationTokenService.validateAndConsume("qualquer-token");

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(token.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("should reject an unknown token")
        void shouldRejectUnknownToken() {
            given(emailVerificationTokenRepository.findByTokenHash(any(String.class))).willReturn(Optional.empty());

            assertThatThrownBy(() -> emailVerificationTokenService.validateAndConsume("token-desconhecido"))
                    .isInstanceOf(InvalidEmailVerificationTokenException.class);
        }

        @Test
        @DisplayName("should reject a token that was already used")
        void shouldRejectUsedToken() {
            EmailVerificationToken token = buildToken();
            token.setUsedAt(OffsetDateTime.now());
            given(emailVerificationTokenRepository.findByTokenHash(any(String.class))).willReturn(Optional.of(token));

            assertThatThrownBy(() -> emailVerificationTokenService.validateAndConsume("token-usado"))
                    .isInstanceOf(InvalidEmailVerificationTokenException.class);
        }

        @Test
        @DisplayName("should reject an expired token")
        void shouldRejectExpiredToken() {
            EmailVerificationToken token = buildToken();
            token.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
            given(emailVerificationTokenRepository.findByTokenHash(any(String.class))).willReturn(Optional.of(token));

            assertThatThrownBy(() -> emailVerificationTokenService.validateAndConsume("token-expirado"))
                    .isInstanceOf(ExpiredEmailVerificationTokenException.class);
        }
    }

    @Nested
    @DisplayName("purgeExhaustedTokens")
    class PurgeExhaustedTokens {

        @Test
        @DisplayName("should delete expired unused tokens and used tokens past the retention window")
        void shouldPurge() {
            given(emailVerificationTokenRepository.deleteExhaustedTokens(any(OffsetDateTime.class),
                    any(OffsetDateTime.class))).willReturn(2);

            assertThat(emailVerificationTokenService.purgeExhaustedTokens()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("should wipe every verification token of the user")
    void shouldWipeEveryTokenOfTheUser() {
        given(emailVerificationTokenRepository.deleteAllByUserId(7L)).willReturn(1);

        assertThat(emailVerificationTokenService.deleteAllByUserId(7L)).isEqualTo(1);
    }
}
