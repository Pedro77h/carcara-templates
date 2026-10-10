package br.com.template.core.modules.auth.schedulers;

import br.com.template.core.modules.auth.services.AuthAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class AuthAttemptCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuthAttemptCleanupScheduler.class);

    private final AuthAttemptService authAttemptService;
    private final int retentionHours;

    public AuthAttemptCleanupScheduler(AuthAttemptService authAttemptService,
                                       @Value("${app.auth.rate-limit.retention-hours:8}") int retentionHours) {
        this.authAttemptService = authAttemptService;
        this.retentionHours = retentionHours;
    }

    @Scheduled(fixedDelayString = "${app.auth.rate-limit.cleanup-interval-ms:3600000}")
    public void purgeOldAttempts() {
        int purged = authAttemptService.purgeAttemptedBefore(OffsetDateTime.now().minusHours(retentionHours));

        if (purged > 0) {
            log.info("Purged {} authentication attempts older than {} hours", purged, retentionHours);
        }
    }
}
