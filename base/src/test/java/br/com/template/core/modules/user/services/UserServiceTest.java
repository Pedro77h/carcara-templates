package br.com.template.core.modules.user.services;

import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .fullName("Maria da Silva")
                .email("maria@example.com")
                .principalRole(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should encode the password before saving when a password is given")
        void shouldEncodePassword_whenPasswordGiven() {
            User user = buildUser();
            user.setPassword("senha-em-texto-puro");
            given(passwordEncoder.encode("senha-em-texto-puro")).willReturn("senha-codificada");
            given(userRepository.save(user)).willReturn(user);

            userService.create(user);

            assertThat(user.getPassword()).isEqualTo("senha-codificada");
            then(userRepository).should().save(user);
        }

        @Test
        @DisplayName("should not attempt to encode when the password is blank")
        void shouldNotEncode_whenPasswordBlank() {
            User user = buildUser();
            user.setPassword("   ");
            given(userRepository.save(user)).willReturn(user);

            userService.create(user);

            assertThat(user.getPassword()).isEqualTo("   ");
            then(passwordEncoder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("should stamp the creation instant when it is not set")
        void shouldStampCreatedAt_whenMissing() {
            User user = buildUser();
            given(userRepository.save(user)).willReturn(user);

            userService.create(user);

            assertThat(user.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should keep the creation instant already set by the caller")
        void shouldKeepCreatedAt_whenAlreadySet() {
            User user = buildUser();
            OffsetDateTime createdAt = OffsetDateTime.now().minusDays(3);
            user.setCreatedAt(createdAt);
            given(userRepository.save(user)).willReturn(user);

            userService.create(user);

            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should stamp the update instant before saving")
        void shouldStampUpdatedAt() {
            User user = buildUser();
            given(userRepository.save(user)).willReturn(user);

            userService.update(user);

            assertThat(user.getUpdatedAt()).isNotNull();
            then(userRepository).should().save(user);
        }
    }

    @Nested
    @DisplayName("lookups")
    class Lookups {

        @Test
        @DisplayName("should return the user found by id")
        void shouldReturnUserById() {
            User user = buildUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThat(userService.findById(1L)).isEqualTo(user);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when no user has the given id")
        void shouldThrow_whenIdNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should return the user found by email")
        void shouldReturnUserByEmail() {
            User user = buildUser();
            given(userRepository.findByEmailIgnoreCase("maria@example.com")).willReturn(Optional.of(user));

            assertThat(userService.findByEmail("maria@example.com")).isEqualTo(user);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when no user has the given email")
        void shouldThrow_whenEmailNotFound() {
            given(userRepository.findByEmailIgnoreCase("ninguem@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("ninguem@example.com"))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should return an empty optional when the email is unknown")
        void shouldReturnEmptyOptional_whenEmailUnknown() {
            given(userRepository.findByEmailIgnoreCase("ninguem@example.com")).willReturn(Optional.empty());

            assertThat(userService.findByEmailOptional("ninguem@example.com")).isEmpty();
        }

        @Test
        @DisplayName("should delegate the email existence check to the repository")
        void shouldDelegateExistsByEmail() {
            given(userRepository.existsByEmailIgnoreCase("maria@example.com")).willReturn(true);

            assertThat(userService.existsByEmail("maria@example.com")).isTrue();
        }

    }

    @Nested
    @DisplayName("updatePassword")
    class UpdatePassword {

        @Test
        @DisplayName("should encode the new password and stamp the change instant")
        void shouldEncodeAndStamp() {
            User user = buildUser();
            given(passwordEncoder.encode("nova-senha")).willReturn("nova-senha-codificada");

            userService.updatePassword(user, "nova-senha");

            assertThat(user.getPassword()).isEqualTo("nova-senha-codificada");
            assertThat(user.getPasswordChangedAt()).isNotNull();
            then(userRepository).should().save(user);
        }
    }

    @Nested
    @DisplayName("roles")
    class Roles {

        @Test
        @DisplayName("should merge the roles list with the principal role without duplicates")
        void shouldMergeRolesWithoutDuplicates() {
            User user = buildUser();
            user.setRoles(List.of(Role.ROLE_USER, Role.ROLE_ADMIN));

            List<String> result = userService.getRoleNames(user);

            assertThat(result).containsExactly("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("should return only the principal role when the roles list is null")
        void shouldReturnPrincipalRole_whenRolesNull() {
            User user = buildUser();
            user.setRoles(null);

            assertThat(userService.getRoleNames(user)).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should return an empty list when there is no role at all")
        void shouldReturnEmptyList_whenNoRoles() {
            User user = User.builder().id(2L).build();

            assertThat(userService.getRoleNames(user)).isEmpty();
        }

        @Test
        @DisplayName("should expose the role names as granted authorities")
        void shouldExposeAuthorities() {
            User user = buildUser();
            user.setRoles(List.of(Role.ROLE_USER));

            List<SimpleGrantedAuthority> result = userService.getAuthorities(user);

            assertThat(result).containsExactly(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    @Nested
    @DisplayName("markEmailAsVerified")
    class MarkEmailAsVerified {

        @Test
        @DisplayName("should stamp the verification timestamp and save the user")
        void shouldStampVerification() {
            User user = buildUser();

            userService.markEmailAsVerified(user);

            assertThat(user.getEmailVerifiedAt()).isNotNull();
            then(userRepository).should().save(user);
        }
    }


    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should remove the account by its id")
        void shouldRemoveTheAccount() {
            given(userRepository.deleteAccountById(7L)).willReturn(1);

            int result = userService.delete(7L);

            assertThat(result).isEqualTo(1);
            then(userRepository).should().deleteAccountById(7L);
        }
    }
}
