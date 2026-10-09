package br.com.template.core.modules.auth.controller;

import br.com.template.core.core.components.CurrentUserComponent;
import br.com.template.core.modules.auth.components.AuthComponent;
import br.com.template.core.modules.auth.dtos.AuthResponse;
import br.com.template.core.modules.auth.dtos.ForgotPasswordRequest;
import br.com.template.core.modules.auth.dtos.LoginRequest;
import br.com.template.core.modules.auth.dtos.RefreshTokenRequest;
import br.com.template.core.modules.auth.dtos.RegisterRequest;
import br.com.template.core.modules.auth.dtos.ResetPasswordRequest;
import br.com.template.core.modules.auth.dtos.ValidateResetTokenRequest;
import br.com.template.core.modules.auth.dtos.VerifyEmailRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthComponent authComponent;

    @Mock
    private CurrentUserComponent currentUserComponent;

    @Mock
    private HttpServletRequest httpServletRequest;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authComponent, currentUserComponent);
    }

    private RegisterRequest buildRegisterRequest() {
        return new RegisterRequest("Ana Souza", "85999990000", "ana@example.com", "senha-forte");
    }

    @Test
    @DisplayName("should answer 202 without a body on register so it never reveals who has an account")
    void shouldAnswerAcceptedOnRegister() {
        RegisterRequest request = buildRegisterRequest();
        given(authComponent.register(request)).willReturn(Optional.empty());

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("should answer 200 with the token pair on register when email verification is disabled")
    void shouldAnswerOkOnRegisterWhenVerificationDisabled() {
        RegisterRequest request = buildRegisterRequest();
        AuthResponse authResponse = AuthResponse.builder().accessToken("access-token").build();
        given(authComponent.register(request)).willReturn(Optional.of(authResponse));

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }

    @Test
    @DisplayName("should answer 200 with the token pair on email verification")
    void shouldAnswerOkOnVerifyEmail() {
        VerifyEmailRequest request = new VerifyEmailRequest("token-verificacao");
        AuthResponse authResponse = AuthResponse.builder().accessToken("access-token").build();
        given(authComponent.verifyEmail(request)).willReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.verifyEmail(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }

    @Test
    @DisplayName("should pass the remote address to the component on login")
    void shouldPassRemoteAddressOnLogin() {
        LoginRequest request = new LoginRequest("ana@example.com", "senha-forte");
        AuthResponse authResponse = AuthResponse.builder().accessToken("access-token").build();
        given(httpServletRequest.getRemoteAddr()).willReturn("187.19.20.21");
        given(authComponent.login(request, "187.19.20.21")).willReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.login(request, httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }

    @Test
    @DisplayName("should answer 200 with a new token pair on refresh")
    void shouldAnswerOkOnRefresh() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        AuthResponse authResponse = AuthResponse.builder().accessToken("novo-access-token").build();
        given(authComponent.refresh(request)).willReturn(authResponse);

        assertThat(authController.refresh(request).getBody()).isEqualTo(authResponse);
    }

    @Test
    @DisplayName("should pass the remote address to the component on forgot password")
    void shouldPassRemoteAddressOnForgotPassword() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("ana@example.com");
        given(httpServletRequest.getRemoteAddr()).willReturn("187.19.20.21");

        ResponseEntity<Void> response = authController.forgotPassword(request, httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(authComponent).should().forgotPassword(request, "187.19.20.21");
    }

    @Test
    @DisplayName("should delegate the reset token validation to the component")
    void shouldDelegateResetTokenValidation() {
        ResponseEntity<Void> response =
                authController.isResetPasswordTokenValid(new ValidateResetTokenRequest("token-gerado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(authComponent).should().isResetPasswordValid("token-gerado");
    }

    @Test
    @DisplayName("should delegate the password reset to the component")
    void shouldDelegatePasswordReset() {
        ResetPasswordRequest request = new ResetPasswordRequest("token-gerado", "nova-senha");

        ResponseEntity<Void> response = authController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(authComponent).should().resetPassword(request);
    }

    @Test
    @DisplayName("should log out the authenticated user taken from the token, never from the payload")
    void shouldLogOutTheAuthenticatedUser() {
        given(currentUserComponent.getCurrentUserId()).willReturn(7L);

        ResponseEntity<Void> response = authController.logout();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        then(authComponent).should().logout(7L);
    }
}
