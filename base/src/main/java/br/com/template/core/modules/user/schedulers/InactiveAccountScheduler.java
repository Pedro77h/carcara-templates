package br.com.template.core.modules.user.schedulers;

import br.com.template.core.modules.user.components.UserComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class InactiveAccountScheduler {

    private static final Logger log = LoggerFactory.getLogger(InactiveAccountScheduler.class);

    private final UserComponent userComponent;
    private final int inactivityMonths;

    public InactiveAccountScheduler(UserComponent userComponent,
                                    @Value("${app.users.inactivity-months:6}") int inactivityMonths) {
        this.userComponent = userComponent;
        this.inactivityMonths = inactivityMonths;
    }

    @Scheduled(fixedDelayString = "${app.users.inactivity-check-interval-ms:86400000}")
    public void deactivateInactiveAccounts() {
        int deactivated = userComponent.deactivateInactiveSince(
                OffsetDateTime.now().minusMonths(inactivityMonths));

        if (deactivated > 0) {
            log.info("Deactivated {} accounts inactive for more than {} months", deactivated, inactivityMonths);
        }
    }
}
