package br.com.template.core.modules.user.schedulers;

import br.com.template.core.modules.user.components.UserComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InactiveAccountSchedulerTest {

    @Mock
    private UserComponent userComponent;

    @Test
    @DisplayName("should deactivate the accounts inactive for longer than the configured window")
    void shouldDeactivateInactiveAccounts() {
        given(userComponent.deactivateInactiveSince(any(OffsetDateTime.class))).willReturn(3);
        InactiveAccountScheduler scheduler = new InactiveAccountScheduler(userComponent, 6);

        scheduler.deactivateInactiveAccounts();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        then(userComponent).should().deactivateInactiveSince(captor.capture());
        assertThat(captor.getValue()).isBefore(OffsetDateTime.now().minusMonths(6).plusMinutes(1));
        assertThat(captor.getValue()).isAfter(OffsetDateTime.now().minusMonths(6).minusMinutes(1));
    }

    @Test
    @DisplayName("should stay quiet when there is nothing to deactivate")
    void shouldStayQuietWithoutInactiveAccounts() {
        given(userComponent.deactivateInactiveSince(any(OffsetDateTime.class))).willReturn(0);
        InactiveAccountScheduler scheduler = new InactiveAccountScheduler(userComponent, 6);

        scheduler.deactivateInactiveAccounts();

        then(userComponent).should().deactivateInactiveSince(any(OffsetDateTime.class));
    }
}
