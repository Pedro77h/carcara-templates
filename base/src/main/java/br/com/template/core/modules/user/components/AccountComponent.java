package br.com.template.core.modules.user.components;

import br.com.template.core.modules.auth.components.AuthComponent;
import br.com.template.core.modules.auth.dtos.AuthResponse;
import br.com.template.core.modules.user.dtos.ChangePasswordRequest;
import br.com.template.core.modules.user.dtos.DeleteAccountRequest;
import br.com.template.core.modules.user.dtos.MeDto;
import br.com.template.core.modules.user.dtos.UpdateProfileRequest;
import br.com.template.core.modules.user.mappers.UserMapper;
import br.com.template.core.modules.user.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountComponent {

    private final UserComponent userComponent;
    private final UserMapper userMapper;
    private final AuthComponent authComponent;

    @Autowired
    public AccountComponent(UserComponent userComponent, UserMapper userMapper, AuthComponent authComponent) {
        this.userComponent = userComponent;
        this.userMapper = userMapper;
        this.authComponent = authComponent;
    }

    public MeDto findProfile(Long userId) {
        return userMapper.toMeDto(userComponent.findById(userId));
    }

    public MeDto updateProfile(Long userId, UpdateProfileRequest updateProfileRequest) {
        User user = userMapper.applyProfileUpdate(userComponent.findById(userId), updateProfileRequest);

        return userMapper.toMeDto(userComponent.update(user));
    }

    public AuthResponse changePassword(Long userId, ChangePasswordRequest changePasswordRequest) {
        User user = userComponent.findById(userId);
        authComponent.verifyPassword(user, changePasswordRequest.currentPassword());

        userComponent.updatePassword(user, changePasswordRequest.newPassword());
        userComponent.revokeTokens(user);

        return authComponent.issueTokens(user);
    }

    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest deleteAccountRequest) {
        User user = userComponent.findById(userId);
        authComponent.verifyPassword(user, deleteAccountRequest.password());

        authComponent.deleteTokensByUserId(userId);
        userComponent.delete(userId);
    }
}
