package br.com.template.core.modules.user.components;

import br.com.template.core.modules.auth.components.AuthComponent;
import br.com.template.core.modules.auth.dtos.AuthResponse;
import br.com.template.core.modules.auth.exceptions.InvalidAuthenticationException;
import br.com.template.core.modules.user.dtos.ChangePasswordRequest;
import br.com.template.core.modules.user.dtos.DeleteAccountRequest;
import br.com.template.core.modules.user.dtos.MeDto;
import br.com.template.core.modules.user.dtos.UpdateProfileRequest;
import br.com.template.core.modules.user.mappers.UserMapper;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AccountComponentTest {

    @Mock
    private UserComponent userComponent;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthComponent authComponent;

    private AccountComponent accountComponent;

    @BeforeEach
    void setUp() {
        accountComponent = new AccountComponent(userComponent, userMapper, authComponent);
    }

    private User buildUser() {
        return User.builder()
                .id(7L)
                .fullName("Ana Souza")
                .email("ana@example.com")
                .principalRole(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("should answer the profile of the authenticated user")
    void shouldAnswerTheProfile() {
        User user = buildUser();
        MeDto expected = MeDto.builder().id(7L).build();
        given(userComponent.findById(7L)).willReturn(user);
        given(userMapper.toMeDto(user)).willReturn(expected);

        assertThat(accountComponent.findProfile(7L)).isEqualTo(expected);
    }

    @Test
    @DisplayName("should persist the profile correction and answer the updated profile")
    void shouldPersistTheProfileCorrection() {
        User user = buildUser();
        User updated = buildUser();
        MeDto expected = MeDto.builder().id(7L).fullName("Ana Souza Lima").build();
        UpdateProfileRequest request = new UpdateProfileRequest("Ana Souza Lima", "85988887777");

        given(userComponent.findById(7L)).willReturn(user);
        given(userMapper.applyProfileUpdate(user, request)).willReturn(user);
        given(userComponent.update(user)).willReturn(updated);
        given(userMapper.toMeDto(updated)).willReturn(expected);

        assertThat(accountComponent.updateProfile(7L, request)).isEqualTo(expected);
    }

    @Test
    @DisplayName("should revoke every session and issue a new token pair when the password changes")
    void shouldRevokeSessionsOnPasswordChange() {
        User user = buildUser();
        AuthResponse expected = AuthResponse.builder().accessToken("novo-access").build();
        ChangePasswordRequest request = new ChangePasswordRequest("senha-atual", "senha-nova");

        given(userComponent.findById(7L)).willReturn(user);
        given(authComponent.issueTokens(user)).willReturn(expected);

        AuthResponse result = accountComponent.changePassword(7L, request);

        assertThat(result).isEqualTo(expected);
        InOrder inOrder = Mockito.inOrder(authComponent, userComponent);
        inOrder.verify(authComponent).verifyPassword(user, "senha-atual");
        inOrder.verify(userComponent).updatePassword(user, "senha-nova");
        inOrder.verify(userComponent).revokeTokens(user);
    }

    @Test
    @DisplayName("should never change the password when the current one does not match")
    void shouldNeverChangeThePasswordWithWrongCurrentPassword() {
        User user = buildUser();
        given(userComponent.findById(7L)).willReturn(user);
        willThrow(new InvalidAuthenticationException("Invalid credentials"))
                .given(authComponent).verifyPassword(user, "senha-errada");

        assertThatThrownBy(() -> accountComponent.changePassword(7L,
                new ChangePasswordRequest("senha-errada", "senha-nova")))
                .isInstanceOf(InvalidAuthenticationException.class);

        then(userComponent).should(never()).updatePassword(user, "senha-nova");
        then(userComponent).should(never()).revokeTokens(user);
    }

    @Test
    @DisplayName("should delete the auth tokens before the user row")
    void shouldDeleteTokensBeforeTheUser() {
        User user = buildUser();
        given(userComponent.findById(7L)).willReturn(user);

        accountComponent.deleteAccount(7L, new DeleteAccountRequest("senha-atual"));

        InOrder inOrder = Mockito.inOrder(authComponent, userComponent);
        inOrder.verify(authComponent).verifyPassword(user, "senha-atual");
        inOrder.verify(authComponent).deleteTokensByUserId(7L);
        inOrder.verify(userComponent).delete(7L);
    }

    @Test
    @DisplayName("should never delete the account when the password does not match")
    void shouldNeverDeleteWithWrongPassword() {
        User user = buildUser();
        given(userComponent.findById(7L)).willReturn(user);
        willThrow(new InvalidAuthenticationException("Invalid credentials"))
                .given(authComponent).verifyPassword(user, "senha-errada");

        assertThatThrownBy(() -> accountComponent.deleteAccount(7L, new DeleteAccountRequest("senha-errada")))
                .isInstanceOf(InvalidAuthenticationException.class);

        then(userComponent).should(never()).delete(7L);
        then(authComponent).should(never()).deleteTokensByUserId(7L);
    }
}
