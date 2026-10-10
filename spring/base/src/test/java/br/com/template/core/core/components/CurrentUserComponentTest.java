package br.com.template.core.core.components;

import br.com.template.core.config.security.jwt.services.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CurrentUserComponentTest {

    @Mock
    private CurrentUserService currentUserService;

    private CurrentUserComponent currentUserComponent;

    @BeforeEach
    void setUp() {
        currentUserComponent = new CurrentUserComponent(currentUserService);
    }

    @Test
    @DisplayName("should expose the authenticated user id")
    void shouldExposeAuthenticatedUserId() {
        given(currentUserService.getCurrentUserId()).willReturn(7L);

        assertThat(currentUserComponent.getCurrentUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("should expose the optional user id of an anonymous request")
    void shouldExposeOptionalUserId() {
        given(currentUserService.getCurrentUserIdOptional()).willReturn(Optional.empty());

        assertThat(currentUserComponent.findCurrentUserId()).isEmpty();
    }
}
