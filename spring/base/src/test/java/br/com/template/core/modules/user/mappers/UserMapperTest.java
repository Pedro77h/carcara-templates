package br.com.template.core.modules.user.mappers;

import br.com.template.core.modules.auth.dtos.RegisterRequest;
import br.com.template.core.modules.user.dtos.MeDto;
import br.com.template.core.modules.user.dtos.UpdateProfileRequest;
import br.com.template.core.modules.user.dtos.UserDto;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    private User buildUser() {
        return User.builder()
                .id(1L)
                .fullName("Ana Souza")
                .email("ana@example.com")
                .phone("85999990000")
                .password("hash-da-senha")
                .principalRole(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("toUserDto")
    class ToUserDto {

        @Test
        @DisplayName("should copy the exposed user fields into the dto")
        void shouldCopyExposedFields() {
            UserDto result = userMapper.toUserDto(buildUser());

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFullName()).isEqualTo("Ana Souza");
            assertThat(result.getEmail()).isEqualTo("ana@example.com");
            assertThat(result.getPhone()).isEqualTo("85999990000");
            assertThat(result.getPrincipalRole()).isEqualTo(Role.ROLE_USER);
        }

        @Test
        @DisplayName("should never expose the password")
        void shouldNeverExposeThePassword() {
            assertThat(UserDto.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .doesNotContain("password");
        }
    }

    @Nested
    @DisplayName("toMeDto")
    class ToMeDto {

        @Test
        @DisplayName("should flag the profile as verified when the email was confirmed")
        void shouldFlagVerifiedProfile() {
            OffsetDateTime createdAt = OffsetDateTime.now().minusDays(3);
            User user = buildUser();
            user.setEmailVerifiedAt(OffsetDateTime.now());
            user.setCreatedAt(createdAt);

            MeDto result = userMapper.toMeDto(user);

            assertThat(result.getFullName()).isEqualTo("Ana Souza");
            assertThat(result.isEmailVerified()).isTrue();
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("should flag the profile as unverified while the email was not confirmed")
        void shouldFlagUnverifiedProfile() {
            MeDto result = userMapper.toMeDto(buildUser());

            assertThat(result.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("should never expose the password")
        void shouldNeverExposeThePassword() {
            assertThat(MeDto.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .doesNotContain("password");
        }
    }

    @Nested
    @DisplayName("applyProfileUpdate")
    class ApplyProfileUpdate {

        @Test
        @DisplayName("should trim the name and keep the identity fields untouched")
        void shouldTrimTheNameAndKeepIdentity() {
            User result = userMapper.applyProfileUpdate(buildUser(),
                    new UpdateProfileRequest("  Ana Souza Lima  ", "85988887777"));

            assertThat(result.getFullName()).isEqualTo("Ana Souza Lima");
            assertThat(result.getPhone()).isEqualTo("85988887777");
            assertThat(result.getEmail()).isEqualTo("ana@example.com");
        }
    }

    @Nested
    @DisplayName("toUserEntity")
    class ToUserEntity {

        @Test
        @DisplayName("should build an active user with the ROLE_USER role from the register request")
        void shouldBuildActiveUserWithUserRole() {
            RegisterRequest request = new RegisterRequest("  Ana Souza  ", "85999990000",
                    "  ANA@Example.com ", "senha-forte");

            User result = userMapper.toUserEntity(request);

            assertThat(result.getFullName()).isEqualTo("Ana Souza");
            assertThat(result.getEmail()).isEqualTo("ana@example.com");
            assertThat(result.getPhone()).isEqualTo("85999990000");
            assertThat(result.getPassword()).isEqualTo("senha-forte");
            assertThat(result.getPrincipalRole()).isEqualTo(Role.ROLE_USER);
            assertThat(result.getRoles()).containsExactly(Role.ROLE_USER);
            assertThat(result.isActive()).isTrue();
        }
    }
}
