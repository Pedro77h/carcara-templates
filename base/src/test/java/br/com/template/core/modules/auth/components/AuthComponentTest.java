package br.com.template.core.modules.auth.components;

import br.com.template.core.config.security.jwt.services.JwtService;
import br.com.template.core.modules.auth.dtos.AuthResponse;
import br.com.template.core.modules.auth.dtos.ForgotPasswordRequest;
import br.com.template.core.modules.auth.dtos.LoginRequest;
import br.com.template.core.modules.auth.dtos.RefreshTokenRequest;
import br.com.template.core.modules.auth.dtos.RegisterRequest;
import br.com.template.core.modules.auth.dtos.ResetPasswordRequest;
import br.com.template.core.modules.auth.dtos.VerifyEmailRequest;
import br.com.template.core.modules.auth.exceptions.InvalidAuthenticationException;
import br.com.template.core.modules.auth.exceptions.TooManyAuthAttemptsException;
import br.com.template.core.modules.auth.models.AuthAttemptScope;
import br.com.template.core.modules.auth.services.AuthAttemptService;
import br.com.template.core.modules.auth.services.AuthService;
import br.com.template.core.modules.user.components.UserComponent;
import br.com.template.core.modules.user.mappers.UserMapper;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthComponentTest {

    @Mock
    private UserComponent userComponent;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthAttemptService authAttemptService;

    private AuthComponent authComponentOf(boolean emailVerificationRequired) {
        return new AuthComponent(userComponent, userMapper, authService, jwtService, authAttemptService,
                emailVerificationRequired);
    }

    private User buildVerifiedUser() {
        return User.builder()
                .id(7L)
                .fullName("Ana Souza")
                .email("ana@example.com")
                .password("hash-da-senha")
                .principalRole(Role.ROLE_USER)
                .emailVerifiedAt(OffsetDateTime.now())
                .build();
    }

    private void givenTokensAreIssuedFor(User user) {
        given(userComponent.getRoleNames(user)).willReturn(List.of("ROLE_USER"));
        given(authService.buildAuthResponse(user, List.of("ROLE_USER")))
                .willReturn(AuthResponse.builder().accessToken("access").refreshToken("refresh").build());
    }

    @Nested
    @DisplayName("register")
    class Register {

        private final RegisterRequest request =
                new RegisterRequest("Ana Souza", "85999990000", "ana@example.com", "senha-forte");

        @Test
        @DisplayName("should create the user and send the verification email without issuing tokens")
        void shouldCreateAndSendVerification() {
            User user = buildVerifiedUser();
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.empty());
            given(userMapper.toUserEntity(request)).willReturn(user);
            given(userComponent.create(user)).willReturn(user);

            Optional<AuthResponse> result = authComponentOf(true).register(request);

            assertThat(result).isEmpty();
            then(authService).should().sendEmailVerification(user);
        }

        @Test
        @DisplayName("should issue tokens right away when email verification is disabled")
        void shouldIssueTokensWhenVerificationDisabled() {
            User user = buildVerifiedUser();
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.empty());
            given(userMapper.toUserEntity(request)).willReturn(user);
            given(userComponent.create(user)).willReturn(user);
            givenTokensAreIssuedFor(user);

            Optional<AuthResponse> result = authComponentOf(false).register(request);

            assertThat(result).isPresent();
            then(userComponent).should().markEmailAsVerified(user);
            then(authService).should(never()).sendEmailVerification(user);
        }

        @Test
        @DisplayName("should never create a second account nor reveal that the email is taken")
        void shouldNeverRevealTakenEmail() {
            User existing = buildVerifiedUser();
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.of(existing));

            Optional<AuthResponse> result = authComponentOf(true).register(request);

            assertThat(result).isEmpty();
            then(authService).should().sendRegistrationAttemptNotice(existing);
            then(userComponent).should(never()).create(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private final LoginRequest request = new LoginRequest("ana@example.com", "senha-forte");

        @Test
        @DisplayName("should issue tokens and clear the attempt counter on success")
        void shouldIssueTokensOnSuccess() {
            User user = buildVerifiedUser();
            given(authAttemptService.isBlocked(AuthAttemptScope.LOGIN_EMAIL, "ana@example.com")).willReturn(false);
            given(authAttemptService.isBlocked(AuthAttemptScope.LOGIN_ORIGIN, "10.0.0.1")).willReturn(false);
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.of(user));
            givenTokensAreIssuedFor(user);

            AuthResponse result = authComponentOf(true).login(request, "10.0.0.1");

            assertThat(result.getAccessToken()).isEqualTo("access");
            then(authAttemptService).should().clear(AuthAttemptScope.LOGIN_EMAIL, "ana@example.com");
            then(userComponent).should().registerLogin(user);
        }

        @Test
        @DisplayName("should refuse before touching the database when the identifier is blocked")
        void shouldRefuseWhenBlocked() {
            given(authAttemptService.isBlocked(AuthAttemptScope.LOGIN_EMAIL, "ana@example.com")).willReturn(true);

            assertThatThrownBy(() -> authComponentOf(true).login(request, "10.0.0.1"))
                    .isInstanceOf(TooManyAuthAttemptsException.class);

            then(userComponent).should(never()).findByEmailOptional("ana@example.com");
        }

        @Test
        @DisplayName("should spend the hashing time and record the attempt for an unknown email")
        void shouldSpendTimeForUnknownEmail() {
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authComponentOf(true).login(request, "10.0.0.1"))
                    .isInstanceOf(InvalidAuthenticationException.class);

            then(authService).should().spendPasswordVerificationTime("senha-forte");
            then(authAttemptService).should().record(AuthAttemptScope.LOGIN_EMAIL, "ana@example.com");
            then(authAttemptService).should().record(AuthAttemptScope.LOGIN_ORIGIN, "10.0.0.1");
        }

        @Test
        @DisplayName("should record the failed attempt when the password does not match")
        void shouldRecordFailedPassword() {
            User user = buildVerifiedUser();
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.of(user));
            org.mockito.BDDMockito.willThrow(new InvalidAuthenticationException("Invalid credentials"))
                    .given(authService).verifyPassword("senha-forte", user);

            assertThatThrownBy(() -> authComponentOf(true).login(request, "10.0.0.1"))
                    .isInstanceOf(InvalidAuthenticationException.class);

            then(authAttemptService).should().record(AuthAttemptScope.LOGIN_EMAIL, "ana@example.com");
        }

        @Test
        @DisplayName("should refuse an account whose email was never confirmed")
        void shouldRefuseUnverifiedAccount() {
            User user = buildVerifiedUser();
            user.setEmailVerifiedAt(null);
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authComponentOf(true).login(request, "10.0.0.1"))
                    .isInstanceOf(InvalidAuthenticationException.class);

            then(userComponent).should(never()).registerLogin(user);
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("should mark the email as verified and issue tokens")
        void shouldMarkVerifiedAndIssueTokens() {
            User user = buildVerifiedUser();
            given(authService.consumeEmailVerificationToken("token-bruto")).willReturn(user);
            givenTokensAreIssuedFor(user);

            AuthResponse result = authComponentOf(true).verifyEmail(new VerifyEmailRequest("token-bruto"));

            assertThat(result.getAccessToken()).isEqualTo("access");
            then(userComponent).should().markEmailAsVerified(user);
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("should issue a new token pair for a valid refresh token")
        void shouldIssueNewPair() {
            User user = buildVerifiedUser();
            JwtService.TokenClaims claims = new JwtService.TokenClaims(7L, "ana@example.com",
                    List.of("ROLE_USER"), "refresh", Instant.now(), 0);
            given(jwtService.parseRefreshToken("refresh-token")).willReturn(claims);
            given(userComponent.findById(7L)).willReturn(user);
            givenTokensAreIssuedFor(user);

            AuthResponse result = authComponentOf(true).refresh(new RefreshTokenRequest("refresh-token"));

            assertThat(result.getRefreshToken()).isEqualTo("refresh");
            then(authService).should().ensureRefreshAllowed(claims, user);
        }

        @Test
        @DisplayName("should answer invalid credentials when the refresh token is rejected")
        void shouldAnswerInvalidForRejectedToken() {
            given(jwtService.parseRefreshToken("refresh-token"))
                    .willThrow(new IllegalArgumentException("Invalid refresh token type"));

            assertThatThrownBy(() -> authComponentOf(true).refresh(new RefreshTokenRequest("refresh-token")))
                    .isInstanceOf(InvalidAuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        private final ForgotPasswordRequest request = new ForgotPasswordRequest("ana@example.com");

        @Test
        @DisplayName("should send the reset email and record the attempt")
        void shouldSendResetEmail() {
            User user = buildVerifiedUser();
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.of(user));

            authComponentOf(true).forgotPassword(request, "10.0.0.1");

            then(authService).should().sendPasswordReset(user);
            then(authAttemptService).should().record(AuthAttemptScope.PASSWORD_RESET_EMAIL, "ana@example.com");
        }

        @Test
        @DisplayName("should stay silent for an unknown email")
        void shouldStaySilentForUnknownEmail() {
            given(userComponent.findByEmailOptional("ana@example.com")).willReturn(Optional.empty());

            authComponentOf(true).forgotPassword(request, "10.0.0.1");

            then(authService).should(never()).sendPasswordReset(any());
        }

        @Test
        @DisplayName("should do nothing once the identifier is blocked")
        void shouldDoNothingWhenBlocked() {
            given(authAttemptService.isBlocked(AuthAttemptScope.PASSWORD_RESET_EMAIL, "ana@example.com"))
                    .willReturn(true);

            authComponentOf(true).forgotPassword(request, "10.0.0.1");

            then(userComponent).should(never()).findByEmailOptional("ana@example.com");
            then(authAttemptService).should(never())
                    .record(AuthAttemptScope.PASSWORD_RESET_EMAIL, "ana@example.com");
        }
    }

    @Nested
    @DisplayName("resetPassword e logout")
    class ResetPasswordAndLogout {

        @Test
        @DisplayName("should consume the token and set the new password")
        void shouldConsumeTokenAndSetPassword() {
            User user = buildVerifiedUser();
            given(authService.consumeResetToken("token-bruto")).willReturn(user);

            authComponentOf(true).resetPassword(new ResetPasswordRequest("token-bruto", "senha-nova"));

            then(userComponent).should().updatePassword(user, "senha-nova");
        }

        @Test
        @DisplayName("should revoke every session on logout")
        void shouldRevokeSessionsOnLogout() {
            User user = buildVerifiedUser();
            given(userComponent.findById(7L)).willReturn(user);

            authComponentOf(true).logout(7L);

            then(userComponent).should().revokeTokens(user);
        }

        @Test
        @DisplayName("should delegate the reset token validation")
        void shouldDelegateResetTokenValidation() {
            authComponentOf(true).isResetPasswordValid("token-bruto");

            then(authService).should().validateResetToken("token-bruto");
        }
    }
}
