package br.com.template.core.config.security.jwt.services;

import br.com.template.core.config.security.jwt.models.AuthenticatedUserPrincipal;
import br.com.template.core.modules.user.components.UserComponent;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserComponent userComponent;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService(userComponent);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should return the authenticated user loaded from the security context principal")
    void shouldReturnAuthenticatedUser() {
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(1L, "maria@example.com", List.of("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        User user = User.builder().id(1L).principalRole(Role.ROLE_USER).build();
        given(userComponent.findById(1L)).willReturn(user);

        User result = currentUserService.getCurrentUser();

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("should throw IllegalStateException when there is no authentication in the context")
    void shouldThrowIllegalStateException_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when the principal is not AuthenticatedUserPrincipal")
    void shouldThrowIllegalStateException_whenUnexpectedPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-principal", null, List.of()));

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should return the authenticated user id straight from the principal")
    void shouldReturnAuthenticatedUserId() {
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(7L, "maria@example.com", List.of("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        Long result = currentUserService.getCurrentUserId();

        assertThat(result).isEqualTo(7L);
        then(userComponent).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("should throw IllegalStateException on getCurrentUserId when there is no authentication")
    void shouldThrowIllegalStateExceptionOnUserId_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> currentUserService.getCurrentUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated user not found");
    }

    @Test
    @DisplayName("should return the authenticated user id as optional")
    void shouldReturnOptionalUserId() {
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(7L, "maria@example.com", List.of("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        assertThat(currentUserService.getCurrentUserIdOptional()).contains(7L);
    }

    @Test
    @DisplayName("should return an empty optional when there is no authentication")
    void shouldReturnEmptyOptionalWithoutAuthentication() {
        SecurityContextHolder.clearContext();

        assertThat(currentUserService.getCurrentUserIdOptional()).isEmpty();
    }

    @Test
    @DisplayName("should return an empty optional when the principal is not AuthenticatedUserPrincipal")
    void shouldReturnEmptyOptionalForUnexpectedPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThat(currentUserService.getCurrentUserIdOptional()).isEmpty();
    }
}
