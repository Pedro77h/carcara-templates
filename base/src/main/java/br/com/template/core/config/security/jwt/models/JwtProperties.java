package br.com.template.core.config.security.jwt.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenExpirationMinutes,
        long refreshTokenExpirationDays
) {
}
