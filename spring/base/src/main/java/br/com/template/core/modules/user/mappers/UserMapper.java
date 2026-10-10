package br.com.template.core.modules.user.mappers;

import br.com.template.core.modules.auth.dtos.RegisterRequest;
import br.com.template.core.modules.user.dtos.MeDto;
import br.com.template.core.modules.user.dtos.UpdateProfileRequest;
import br.com.template.core.modules.user.dtos.UserDto;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class UserMapper {

    public UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .principalRole(user.getPrincipalRole())
                .build();
    }

    public MeDto toMeDto(User user) {
        return MeDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .principalRole(user.getPrincipalRole())
                .emailVerified(Objects.nonNull(user.getEmailVerifiedAt()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    public User applyProfileUpdate(User user, UpdateProfileRequest updateProfileRequest) {
        user.setFullName(updateProfileRequest.fullName().trim());
        user.setPhone(updateProfileRequest.phone());

        return user;
    }

    public User toUserEntity(RegisterRequest registerRequest) {
        return User.builder()
                .fullName(registerRequest.fullName().trim())
                .email(registerRequest.email().strip().toLowerCase())
                .phone(registerRequest.phone())
                .password(registerRequest.password())
                .principalRole(Role.ROLE_USER)
                .roles(List.of(Role.ROLE_USER))
                .active(true)
                .build();
    }
}
