package br.com.template.core.modules.user.controller;

import br.com.template.core.core.components.CurrentUserComponent;
import br.com.template.core.modules.auth.dtos.AuthResponse;
import br.com.template.core.modules.user.components.AccountComponent;
import br.com.template.core.modules.user.dtos.ChangePasswordRequest;
import br.com.template.core.modules.user.dtos.DeleteAccountRequest;
import br.com.template.core.modules.user.dtos.MeDto;
import br.com.template.core.modules.user.dtos.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Perfil e conta", description = "Cadastro, senha e exclusão da conta do usuário autenticado")
public class MeController {

    private final AccountComponent accountComponent;
    private final CurrentUserComponent currentUserComponent;

    public MeController(AccountComponent accountComponent, CurrentUserComponent currentUserComponent) {
        this.accountComponent = accountComponent;
        this.currentUserComponent = currentUserComponent;
    }

    @GetMapping
    @Operation(summary = "Consultar o próprio cadastro",
            description = "Devolve os dados de cadastro do usuário autenticado")
    public ResponseEntity<MeDto> findProfile() {
        return ResponseEntity.ok(accountComponent.findProfile(currentUserComponent.getCurrentUserId()));
    }

    @PatchMapping
    @Operation(summary = "Corrigir o próprio cadastro",
            description = "Atualiza nome e telefone do usuário autenticado. E-mail não é editável")
    public ResponseEntity<MeDto> updateProfile(@Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        return ResponseEntity.ok(
                accountComponent.updateProfile(currentUserComponent.getCurrentUserId(), updateProfileRequest));
    }

    @PostMapping("/password")
    @Operation(summary = "Trocar a senha",
            description = "Troca a senha conferindo a atual, derruba as sessões de todos os aparelhos e "
                    + "devolve um par de tokens novo")
    public ResponseEntity<AuthResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        return ResponseEntity.ok(
                accountComponent.changePassword(currentUserComponent.getCurrentUserId(), changePasswordRequest));
    }

    @DeleteMapping
    @Operation(summary = "Excluir a conta",
            description = "Apaga definitivamente a conta do usuário autenticado, conferindo a senha")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest deleteAccountRequest) {
        accountComponent.deleteAccount(currentUserComponent.getCurrentUserId(), deleteAccountRequest);

        return ResponseEntity.noContent().build();
    }
}
