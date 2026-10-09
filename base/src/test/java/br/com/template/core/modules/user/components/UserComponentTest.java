package br.com.template.core.modules.user.components;

import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserComponentTest {

    @Mock
    private UserService userService;

    private UserComponent userComponent;

    @BeforeEach
    void setUp() {
        userComponent = new UserComponent(userService);
    }

    private User buildUser() {
        return User.builder().id(1L).email("maria@example.com").principalRole(Role.ROLE_USER).build();
    }

    @Test
    @DisplayName("should delegate the creation to the service")
    void shouldDelegateCreate() {
        User user = buildUser();
        given(userService.create(user)).willReturn(user);

        assertThat(userComponent.create(user)).isEqualTo(user);
    }

    @Test
    @DisplayName("should delegate the update to the service")
    void shouldDelegateUpdate() {
        User user = buildUser();
        given(userService.update(user)).willReturn(user);

        assertThat(userComponent.update(user)).isEqualTo(user);
    }

    @Test
    @DisplayName("should delegate the lookup by id to the service")
    void shouldDelegateFindById() {
        User user = buildUser();
        given(userService.findById(1L)).willReturn(user);

        assertThat(userComponent.findById(1L)).isEqualTo(user);
    }

    @Test
    @DisplayName("should delegate the lookup by email to the service")
    void shouldDelegateFindByEmail() {
        User user = buildUser();
        given(userService.findByEmail("maria@example.com")).willReturn(user);

        assertThat(userComponent.findByEmail("maria@example.com")).isEqualTo(user);
    }

    @Test
    @DisplayName("should delegate the optional lookup by email to the service")
    void shouldDelegateFindByEmailOptional() {
        User user = buildUser();
        given(userService.findByEmailOptional("maria@example.com")).willReturn(Optional.of(user));

        assertThat(userComponent.findByEmailOptional("maria@example.com")).contains(user);
    }


    @Test
    @DisplayName("should delegate the email existence check to the service")
    void shouldDelegateExistsByEmail() {
        given(userService.existsByEmail("maria@example.com")).willReturn(true);

        assertThat(userComponent.existsByEmail("maria@example.com")).isTrue();
    }


    @Test
    @DisplayName("should delegate the password update to the service")
    void shouldDelegateUpdatePassword() {
        User user = buildUser();

        userComponent.updatePassword(user, "nova-senha");

        then(userService).should().updatePassword(user, "nova-senha");
    }

    @Test
    @DisplayName("should delegate the role names to the service")
    void shouldDelegateGetRoleNames() {
        User user = buildUser();
        given(userService.getRoleNames(user)).willReturn(List.of("ROLE_USER"));

        assertThat(userComponent.getRoleNames(user)).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("should delegate the authorities to the service")
    void shouldDelegateGetAuthorities() {
        User user = buildUser();
        given(userService.getAuthorities(user)).willReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(userComponent.getAuthorities(user)).containsExactly(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("should delegate the email verification stamp to the service")
    void shouldDelegateMarkEmailAsVerified() {
        User user = User.builder().id(1L).build();

        userComponent.markEmailAsVerified(user);

        then(userService).should().markEmailAsVerified(user);
    }

    @Test
    @DisplayName("should delegate the removal of the account")
    void shouldDelegateTheRemovalOfTheAccount() {
        given(userService.delete(7L)).willReturn(1);

        assertThat(userComponent.delete(7L)).isEqualTo(1);
    }

}
