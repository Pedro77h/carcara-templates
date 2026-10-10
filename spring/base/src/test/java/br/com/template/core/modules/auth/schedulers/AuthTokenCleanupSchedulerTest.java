package br.com.template.core.modules.auth.schedulers;

import br.com.template.core.modules.auth.services.EmailVerificationTokenService;
import br.com.template.core.modules.auth.services.PasswordResetTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthTokenCleanupSchedulerTest {

    @Mock
    private PasswordResetTokenService passwordResetTokenService;
    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;

    private AuthTokenCleanupScheduler authTokenCleanupScheduler;

    @BeforeEach
    void setUp() {
        authTokenCleanupScheduler = new AuthTokenCleanupScheduler(passwordResetTokenService, emailVerificationTokenService);
    }

    @Test
    @DisplayName("should ask both services to purge the exhausted tokens")
    void shouldPurgeExhaustedTokens() {
        given(passwordResetTokenService.purgeExhaustedTokens()).willReturn(3);
        given(emailVerificationTokenService.purgeExhaustedTokens()).willReturn(2);

        authTokenCleanupScheduler.purgeExhaustedTokens();

        then(passwordResetTokenService).should().purgeExhaustedTokens();
        then(emailVerificationTokenService).should().purgeExhaustedTokens();
    }

    @Test
    @DisplayName("should stay quiet when there was nothing to purge")
    void shouldStayQuietWhenNothingPurged() {
        given(passwordResetTokenService.purgeExhaustedTokens()).willReturn(0);
        given(emailVerificationTokenService.purgeExhaustedTokens()).willReturn(0);

        authTokenCleanupScheduler.purgeExhaustedTokens();

        then(passwordResetTokenService).should().purgeExhaustedTokens();
    }
}
