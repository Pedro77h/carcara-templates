package br.com.template.core.modules.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
        long tokenExpirationMinutes,
        String verificationUrl
) {
}
