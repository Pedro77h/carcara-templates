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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Cadastro, login, refresh de token e recuperação de senha")
public class AuthController {

    private final AuthComponent authComponent;
    private final CurrentUserComponent currentUserComponent;

    public AuthController(AuthComponent authComponent, CurrentUserComponent currentUserComponent) {
        this.authComponent = authComponent;
        this.currentUserComponent = currentUserComponent;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastro",
            description = "Cria a conta e envia o e-mail de confirmação. Responde 202 sem corpo, inclusive "
                    + "quando o e-mail já está cadastrado, para não revelar quem tem conta. Com "
                    + "app.auth.email-verification-required=false a conta já nasce confirmada e a resposta "
                    + "é 200 com o par de tokens")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return authComponent.register(registerRequest)
                .<ResponseEntity<AuthResponse>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.accepted().build());
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirmar e-mail",
            description = "Consome o token enviado por e-mail, ativa a conta e devolve o par de tokens")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest verifyEmailRequest) {
        return ResponseEntity.ok(authComponent.verifyEmail(verifyEmailRequest));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica por e-mail e senha e devolve o par de tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authComponent.login(loginRequest, httpServletRequest.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh", description = "Troca um refresh token válido por um novo par de tokens")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authComponent.refresh(refreshTokenRequest));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout",
            description = "Derruba as sessões do usuário autenticado em todos os aparelhos. Todo token "
                    + "emitido antes deste instante deixa de valer")
    public ResponseEntity<Void> logout() {
        authComponent.logout(currentUserComponent.getCurrentUserId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Esqueci minha senha",
            description = "Envia por e-mail o link de redefinição de senha. Responde 200 exista a conta ou "
                    + "não, para não revelar quem tem cadastro")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest,
                                               HttpServletRequest httpServletRequest) {
        authComponent.forgotPassword(forgotPasswordRequest, httpServletRequest.getRemoteAddr());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password/valid")
    @Operation(summary = "Validar token de redefinição",
            description = "Confere se o token de redefinição ainda pode ser usado. O token vai no corpo, e "
                    + "não no caminho, para não ser registrado no log de acesso do servidor web")
    public ResponseEntity<Void> isResetPasswordTokenValid(
            @Valid @RequestBody ValidateResetTokenRequest validateResetTokenRequest) {
        authComponent.isResetPasswordValid(validateResetTokenRequest.token());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha",
            description = "Consome o token de redefinição e grava a nova senha")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        authComponent.resetPassword(resetPasswordRequest);

        return ResponseEntity.ok().build();
    }
}
