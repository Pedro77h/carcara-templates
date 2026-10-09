package br.com.template.core.modules.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
        long tokenExpirationMinutes,
        String resetUrl
) {
}
