package br.com.template.core.modules.auth.schedulers;

import br.com.template.core.modules.auth.services.EmailVerificationTokenService;
import br.com.template.core.modules.auth.services.PasswordResetTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenCleanupScheduler.class);

    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailVerificationTokenService emailVerificationTokenService;

    public AuthTokenCleanupScheduler(PasswordResetTokenService passwordResetTokenService,
                                              EmailVerificationTokenService emailVerificationTokenService) {
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailVerificationTokenService = emailVerificationTokenService;
    }

    @Scheduled(fixedDelayString = "${app.password-reset.cleanup-interval-ms:3600000}")
    public void purgeExhaustedTokens() {
        int purgedResetTokens = passwordResetTokenService.purgeExhaustedTokens();
        int purgedVerificationTokens = emailVerificationTokenService.purgeExhaustedTokens();

        if (purgedResetTokens > 0 || purgedVerificationTokens > 0) {
            log.info("Purged {} password reset tokens and {} email verification tokens",
                    purgedResetTokens, purgedVerificationTokens);
        }
    }
}
