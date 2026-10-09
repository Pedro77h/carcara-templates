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
import br.com.template.core.modules.user.models.User;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class AuthComponent {

    private final UserComponent userComponent;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthAttemptService authAttemptService;
    private final boolean emailVerificationRequired;

    public AuthComponent(UserComponent userComponent, UserMapper userMapper, AuthService authService,
                         JwtService jwtService, AuthAttemptService authAttemptService,
                         @Value("${app.auth.email-verification-required:true}") boolean emailVerificationRequired) {
        this.userComponent = userComponent;
        this.userMapper = userMapper;
        this.authService = authService;
        this.jwtService = jwtService;
        this.authAttemptService = authAttemptService;
        this.emailVerificationRequired = emailVerificationRequired;
    }

    public Optional<AuthResponse> register(RegisterRequest registerRequest) {
        Optional<User> existingByEmail = userComponent.findByEmailOptional(registerRequest.email());

        if (existingByEmail.isPresent()) {
            authService.sendRegistrationAttemptNotice(existingByEmail.get());
            return Optional.empty();
        }

        User user = userComponent.create(userMapper.toUserEntity(registerRequest));

        if (!emailVerificationRequired) {
            userComponent.markEmailAsVerified(user);
            return Optional.of(buildAuthResponse(user));
        }

        authService.sendEmailVerification(user);
        return Optional.empty();
    }

    public AuthResponse verifyEmail(VerifyEmailRequest verifyEmailRequest) {
        User user = authService.consumeEmailVerificationToken(verifyEmailRequest.token());
        userComponent.markEmailAsVerified(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest loginRequest, String remoteAddress) {
        ensureLoginAllowed(loginRequest.email(), remoteAddress);

        Optional<User> maybeUser = userComponent.findByEmailOptional(loginRequest.email());

        if (maybeUser.isEmpty()) {
            authService.spendPasswordVerificationTime(loginRequest.password());
            recordFailedLogin(loginRequest.email(), remoteAddress);
            throw new InvalidAuthenticationException("Invalid credentials");
        }

        User user = maybeUser.get();

        try {
            authService.verifyPassword(loginRequest.password(), user);

            if (emailVerificationRequired && Objects.isNull(user.getEmailVerifiedAt())) {
                throw new InvalidAuthenticationException("Invalid credentials");
            }
        } catch (RuntimeException exception) {
            recordFailedLogin(loginRequest.email(), remoteAddress);
            throw exception;
        }

        authAttemptService.clear(AuthAttemptScope.LOGIN_EMAIL, loginRequest.email());
        userComponent.registerLogin(user);

        return buildAuthResponse(user);
    }

    public void verifyPassword(User user, String rawPassword) {
        authService.verifyPassword(rawPassword, user);
    }

    public AuthResponse issueTokens(User user) {
        return buildAuthResponse(user);
    }

    public void deleteTokensByUserId(Long userId) {
        authService.deleteTokensByUserId(userId);
    }

    public void logout(Long userId) {
        userComponent.revokeTokens(userComponent.findById(userId));
    }

    public AuthResponse refresh(RefreshTokenRequest refreshTokenRequest) {
        try {
            JwtService.TokenClaims tokenClaims = jwtService.parseRefreshToken(refreshTokenRequest.refreshToken());
            User user = userComponent.findById(tokenClaims.userId());
            authService.ensureRefreshAllowed(tokenClaims, user);

            return buildAuthResponse(user);
        } catch (EntityNotFoundException | IllegalArgumentException | JwtException ex) {
            throw new InvalidAuthenticationException("Invalid refresh token");
        }
    }

    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest, String remoteAddress) {
        if (authAttemptService.isBlocked(AuthAttemptScope.PASSWORD_RESET_EMAIL, forgotPasswordRequest.email())
                || authAttemptService.isBlocked(AuthAttemptScope.PASSWORD_RESET_ORIGIN, remoteAddress)) {
            return;
        }

        authAttemptService.record(AuthAttemptScope.PASSWORD_RESET_EMAIL, forgotPasswordRequest.email());
        authAttemptService.record(AuthAttemptScope.PASSWORD_RESET_ORIGIN, remoteAddress);

        userComponent.findByEmailOptional(forgotPasswordRequest.email())
                .ifPresent(authService::sendPasswordReset);
    }

    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        User user = authService.consumeResetToken(resetPasswordRequest.token());
        userComponent.updatePassword(user, resetPasswordRequest.newPassword());
    }

    public void isResetPasswordValid(String encodedToken) {
        authService.validateResetToken(encodedToken);
    }

    private void ensureLoginAllowed(String email, String remoteAddress) {
        if (authAttemptService.isBlocked(AuthAttemptScope.LOGIN_EMAIL, email)
                || authAttemptService.isBlocked(AuthAttemptScope.LOGIN_ORIGIN, remoteAddress)) {
            throw new TooManyAuthAttemptsException("Too many attempts");
        }
    }

    private void recordFailedLogin(String email, String remoteAddress) {
        authAttemptService.record(AuthAttemptScope.LOGIN_EMAIL, email);
        authAttemptService.record(AuthAttemptScope.LOGIN_ORIGIN, remoteAddress);
    }

    private AuthResponse buildAuthResponse(User user) {
        return authService.buildAuthResponse(user, userComponent.getRoleNames(user));
    }
}
