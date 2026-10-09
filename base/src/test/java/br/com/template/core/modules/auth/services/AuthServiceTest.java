package br.com.template.core.modules.auth.services;

import br.com.template.core.config.security.jwt.services.JwtService;
import br.com.template.core.modules.auth.dtos.AuthResponse;
import br.com.template.core.modules.auth.exceptions.InvalidAuthenticationException;
import br.com.template.core.modules.email.EmailVerificationProperties;
import br.com.template.core.modules.email.PasswordResetProperties;
import br.com.template.core.modules.email.SmtpEmailService;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RESET_URL = "http://localhost:3000/reset-password";

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordResetTokenService passwordResetTokenService;
    @Mock
    private SmtpEmailService smtpEmailService;
    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(passwordEncoder, jwtService, passwordResetTokenService,
                new PasswordResetProperties(30, "http://localhost:3000/reset-password"),
                emailVerificationTokenService,
                new EmailVerificationProperties(1440, "http://localhost:3000/verificar-email"),
                smtpEmailService);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .fullName("Maria da Silva")
                .email("maria@example.com")
                .password("senha-codificada")
                .principalRole(Role.ROLE_USER)
                .roles(List.of(Role.ROLE_USER))
                .build();
    }

    @Nested
    @DisplayName("buildAuthResponse")
    class BuildAuthResponse {

        @Test
        @DisplayName("should assemble the response with the generated token pair")
        void shouldAssembleResponseWithTokens() {
            User user = buildUser();
            given(jwtService.generateAccessToken(user)).willReturn("access-token");
            given(jwtService.generateRefreshToken(user)).willReturn("refresh-token");

            AuthResponse response = authService.buildAuthResponse(user, List.of("ROLE_USER"));

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("maria@example.com");
            assertThat(response.getFullName()).isEqualTo("Maria da Silva");
            assertThat(response.getPrincipalRole()).isEqualTo("ROLE_USER");
            assertThat(response.getRoles()).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should keep a null principal role when the user has none")
        void shouldKeepNullPrincipalRole() {
            User user = buildUser();
            user.setPrincipalRole(null);
            given(jwtService.generateAccessToken(user)).willReturn("access-token");
            given(jwtService.generateRefreshToken(user)).willReturn("refresh-token");

            AuthResponse response = authService.buildAuthResponse(user, List.of("ROLE_USER"));

            assertThat(response.getPrincipalRole()).isNull();
        }
    }

    @Nested
    @DisplayName("verifyPassword")
    class VerifyPassword {

        @Test
        @DisplayName("should pass when the raw password matches the stored hash")
        void shouldPass_whenPasswordMatches() {
            User user = buildUser();
            given(passwordEncoder.matches("senha-forte", "senha-codificada")).willReturn(true);

            assertThatCode(() -> authService.verifyPassword("senha-forte", user)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject when the raw password does not match")
        void shouldReject_whenPasswordDoesNotMatch() {
            User user = buildUser();
            given(passwordEncoder.matches("senha-errada", "senha-codificada")).willReturn(false);

            assertThatThrownBy(() -> authService.verifyPassword("senha-errada", user))
                    .isInstanceOf(InvalidAuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("ensureRefreshAllowed")
    class EnsureRefreshAllowed {

        @Test
        @DisplayName("should pass when the user never changed the password")
        void shouldPass_whenNoPasswordChange() {
            User user = buildUser();
            JwtService.TokenClaims claims = new JwtService.TokenClaims(1L, user.getEmail(),
                    List.of("ROLE_USER"), "refresh", Instant.now(), 0);

            assertThatCode(() -> authService.ensureRefreshAllowed(claims, user)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject a token issued before the last password change")
        void shouldReject_whenTokenIssuedBeforePasswordChange() {
            User user = buildUser();
            user.setPasswordChangedAt(OffsetDateTime.now());
            JwtService.TokenClaims claims = new JwtService.TokenClaims(1L, user.getEmail(),
                    List.of("ROLE_USER"), "refresh", Instant.now().minusSeconds(600), 0);

            assertThatThrownBy(() -> authService.ensureRefreshAllowed(claims, user))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("sendPasswordReset")
    class SendPasswordReset {

        @Test
        @DisplayName("should send the reset link built from the generated token")
        void shouldSendResetLink() {
            User user = buildUser();
            given(passwordResetTokenService.createTokenForUser(user)).willReturn("token-gerado");

            authService.sendPasswordReset(user);

            then(smtpEmailService).should()
                    .sendRecoveryMail("maria@example.com", RESET_URL + "?token=token-gerado", 30);
        }

        @Test
        @DisplayName("should not propagate a failure coming from the mail server")
        void shouldNotPropagateMailFailure() {
            User user = buildUser();
            given(passwordResetTokenService.createTokenForUser(user)).willReturn("token-gerado");
            willThrow(new IllegalStateException("smtp down")).given(smtpEmailService)
                    .sendRecoveryMail(anyString(), anyString(), anyLong());

            assertThatCode(() -> authService.sendPasswordReset(user)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("reset token")
    class ResetToken {

        @Test
        @DisplayName("should consume the token and return the owning user")
        void shouldConsumeToken() {
            User user = buildUser();
            given(passwordResetTokenService.validateAndConsume("token-gerado")).willReturn(user);

            assertThat(authService.consumeResetToken("token-gerado")).isEqualTo(user);
        }

        @Test
        @DisplayName("should delegate the token validation to the reset token service")
        void shouldDelegateTokenValidation() {
            authService.validateResetToken("token-gerado");

            then(passwordResetTokenService).should().validate("token-gerado");
        }
    }

    @Nested
    @DisplayName("email verification")
    class EmailVerification {

        @Test
        @DisplayName("should create the token and send the verification link")
        void shouldSendVerificationLink() {
            User user = User.builder().id(1L).email("maria@example.com").build();
            given(emailVerificationTokenService.createTokenForUser(user)).willReturn("token-verificacao");

            authService.sendEmailVerification(user);

            then(smtpEmailService).should().sendEmailVerificationMail("maria@example.com",
                    "http://localhost:3000/verificar-email?token=token-verificacao", 1440);
        }

        @Test
        @DisplayName("should not propagate a failure to send the verification email")
        void shouldSwallowVerificationSendFailure() {
            User user = User.builder().id(1L).email("maria@example.com").build();
            given(emailVerificationTokenService.createTokenForUser(user)).willReturn("token-verificacao");
            willThrow(new IllegalStateException("smtp fora do ar"))
                    .given(smtpEmailService).sendEmailVerificationMail(any(), any(), anyLong());

            authService.sendEmailVerification(user);
        }

        @Test
        @DisplayName("should warn the owner about a registration attempt without propagating failures")
        void shouldWarnOwnerAboutRegistrationAttempt() {
            User user = User.builder().id(1L).email("maria@example.com").build();

            authService.sendRegistrationAttemptNotice(user);

            then(smtpEmailService).should().sendRegistrationAttemptMail("maria@example.com");
        }

        @Test
        @DisplayName("should swallow a failure while warning about a registration attempt")
        void shouldSwallowAttemptNoticeFailure() {
            User user = User.builder().id(1L).email("maria@example.com").build();
            willThrow(new IllegalStateException("smtp fora do ar"))
                    .given(smtpEmailService).sendRegistrationAttemptMail(any());

            authService.sendRegistrationAttemptNotice(user);
        }

        @Test
        @DisplayName("should delegate the token consumption to the verification service")
        void shouldConsumeVerificationToken() {
            User user = User.builder().id(1L).build();
            given(emailVerificationTokenService.validateAndConsume("token-verificacao")).willReturn(user);

            assertThat(authService.consumeEmailVerificationToken("token-verificacao")).isEqualTo(user);
        }
    }

    @Nested
    @DisplayName("spendPasswordVerificationTime")
    class SpendPasswordVerificationTime {

        @Test
        @DisplayName("should run the encoder against a dummy hash so an unknown email costs the same")
        void shouldRunEncoderAgainstDummyHash() {
            authService.spendPasswordVerificationTime("senha-qualquer");

            then(passwordEncoder).should().matches(eq("senha-qualquer"), any(String.class));
        }
    }

    @Nested
    @DisplayName("session revocation")
    class SessionRevocation {

        @Test
        @DisplayName("should reject a token from a session that was revoked")
        void shouldRejectTokenFromRevokedSession() {
            User user = User.builder().id(1L).email("maria@example.com").sessionVersion(1).build();
            JwtService.TokenClaims claims = new JwtService.TokenClaims(1L, user.getEmail(),
                    List.of("ROLE_USER"), "refresh", Instant.now(), 0);

            assertThat(AuthService.isFromRevokedSession(claims, user)).isTrue();
        }

        @Test
        @DisplayName("should accept a token issued in the current session, whatever the clock says")
        void shouldAcceptTokenFromCurrentSession() {
            User user = User.builder().id(1L).email("maria@example.com").sessionVersion(3).build();
            JwtService.TokenClaims claims = new JwtService.TokenClaims(1L, user.getEmail(),
                    List.of("ROLE_USER"), "access", Instant.now().minusSeconds(3600), 3);

            assertThat(AuthService.isFromRevokedSession(claims, user)).isFalse();
        }
    }

    @Test
    @DisplayName("should wipe the reset and the verification tokens of the user")
    void shouldWipeEveryTokenOfTheUser() {
        authService.deleteTokensByUserId(7L);

        then(passwordResetTokenService).should().deleteAllByUserId(7L);
        then(emailVerificationTokenService).should().deleteAllByUserId(7L);
    }
}
