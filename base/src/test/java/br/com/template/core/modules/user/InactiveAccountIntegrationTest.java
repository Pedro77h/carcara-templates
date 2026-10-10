package br.com.template.core.modules.user;

import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.repositories.UserRepository;
import br.com.template.core.modules.user.services.UserService;
import br.com.template.core.support.CoreTestServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InactiveAccountIntegrationTest extends CoreTestServer {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    private User save(String prefix, OffsetDateTime lastLoginAt, OffsetDateTime createdAt) {
        return userRepository.save(User.builder()
                .fullName(prefix + " da Silva")
                .email(prefix + System.nanoTime() + "@example.com")
                .password("hash")
                .principalRole(Role.ROLE_USER)
                .roles(List.of(Role.ROLE_USER))
                .active(true)
                .createdAt(createdAt)
                .lastLoginAt(lastLoginAt)
                .build());
    }

    private boolean isActive(Long id) {
        return userRepository.findById(id).orElseThrow().isActive();
    }

    @Test
    @DisplayName("should deactivate the account that has not been used for more than six months")
    void shouldDeactivateAccountUnusedForMoreThanSixMonths() {
        Long inactive = save("inativa", OffsetDateTime.now().minusMonths(7), OffsetDateTime.now().minusYears(1)).getId();

        userService.deactivateInactiveSince(OffsetDateTime.now().minusMonths(6));

        assertThat(isActive(inactive)).isFalse();
    }

    @Test
    @DisplayName("should keep the account that was used inside the window")
    void shouldKeepRecentlyUsedAccount() {
        Long recent = save("recente", OffsetDateTime.now().minusMonths(5), OffsetDateTime.now().minusYears(1)).getId();

        userService.deactivateInactiveSince(OffsetDateTime.now().minusMonths(6));

        assertThat(isActive(recent)).isTrue();
    }

    @Test
    @DisplayName("should count from the registration when the user never logged in")
    void shouldCountFromRegistrationWhenNeverLoggedIn() {
        Long neverLoggedRecent = save("nova", null, OffsetDateTime.now().minusMonths(2)).getId();
        Long neverLoggedOld = save("antiga", null, OffsetDateTime.now().minusMonths(8)).getId();

        userService.deactivateInactiveSince(OffsetDateTime.now().minusMonths(6));

        assertThat(isActive(neverLoggedRecent)).isTrue();
        assertThat(isActive(neverLoggedOld)).isFalse();
    }

    @Test
    @DisplayName("should record the login instant so the window can be counted at all")
    void shouldRecordTheLoginInstant() {
        User user = save("maria", null, OffsetDateTime.now().minusMonths(8));

        userService.registerLogin(user);

        assertThat(userRepository.findById(user.getId()).orElseThrow().getLastLoginAt()).isNotNull();
        userService.deactivateInactiveSince(OffsetDateTime.now().minusMonths(6));
        assertThat(isActive(user.getId())).isTrue();
    }
}
