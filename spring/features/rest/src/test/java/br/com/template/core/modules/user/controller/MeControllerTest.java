package br.com.template.core.modules.user.controller;

import br.com.template.core.core.components.CurrentUserComponent;
import br.com.template.core.modules.auth.dtos.AuthResponse;
import br.com.template.core.modules.user.components.AccountComponent;
import br.com.template.core.modules.user.dtos.ChangePasswordRequest;
import br.com.template.core.modules.user.dtos.DeleteAccountRequest;
import br.com.template.core.modules.user.dtos.MeDto;
import br.com.template.core.modules.user.dtos.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    @Mock
    private AccountComponent accountComponent;

    @Mock
    private CurrentUserComponent currentUserComponent;

    private MeController meController;

    @BeforeEach
    void setUp() {
        meController = new MeController(accountComponent, currentUserComponent);
    }

    @Test
    @DisplayName("should answer the profile of the user taken from the token, never from the payload")
    void shouldAnswerTheProfileOfTheTokenOwner() {
        MeDto profile = MeDto.builder().id(7L).build();
        given(currentUserComponent.getCurrentUserId()).willReturn(7L);
        given(accountComponent.findProfile(7L)).willReturn(profile);

        ResponseEntity<MeDto> response = meController.findProfile();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(profile);
    }

    @Test
    @DisplayName("should scope the profile correction to the authenticated user")
    void shouldScopeTheProfileCorrection() {
        UpdateProfileRequest request = new UpdateProfileRequest("Ana Souza Lima", "85988887777");
        MeDto profile = MeDto.builder().id(7L).fullName("Ana Souza Lima").build();
        given(currentUserComponent.getCurrentUserId()).willReturn(7L);
        given(accountComponent.updateProfile(7L, request)).willReturn(profile);

        assertThat(meController.updateProfile(request).getBody()).isEqualTo(profile);
    }

    @Test
    @DisplayName("should answer a new token pair after the password change")
    void shouldAnswerNewTokenPairAfterPasswordChange() {
        ChangePasswordRequest request = new ChangePasswordRequest("senha-atual", "senha-nova");
        AuthResponse authResponse = AuthResponse.builder().accessToken("novo-access").build();
        given(currentUserComponent.getCurrentUserId()).willReturn(7L);
        given(accountComponent.changePassword(7L, request)).willReturn(authResponse);

        ResponseEntity<AuthResponse> response = meController.changePassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }

    @Test
    @DisplayName("should answer 204 after deleting the account of the authenticated user")
    void shouldAnswerNoContentAfterDeletingTheAccount() {
        DeleteAccountRequest request = new DeleteAccountRequest("senha-atual");
        given(currentUserComponent.getCurrentUserId()).willReturn(7L);

        ResponseEntity<Void> response = meController.deleteAccount(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        then(accountComponent).should().deleteAccount(7L, request);
    }
}
