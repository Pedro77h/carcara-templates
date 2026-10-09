package br.com.template.core.core.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

@Component
public class PseudonymHasher {

    private static final Logger log = LoggerFactory.getLogger(PseudonymHasher.class);

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PRODUCTION_PROFILE = "prod";
    private static final int EPHEMERAL_SECRET_BYTES = 32;

    private final SecretKeySpec secretKey;

    public PseudonymHasher(@Value("${app.security.pseudonym-secret:}") String configuredSecret,
                           Environment environment) {
        this.secretKey = new SecretKeySpec(resolveSecret(configuredSecret, environment), ALGORITHM);
    }

    public Optional<String> hash(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return Optional.empty();
        }

        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);

            return Optional.of(HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception exception) {
            log.error("Failed to build pseudonym", exception);

            return Optional.empty();
        }
    }

    private static byte[] resolveSecret(String configuredSecret, Environment environment) {
        if (Objects.nonNull(configuredSecret) && !configuredSecret.isBlank()) {
            return configuredSecret.getBytes(StandardCharsets.UTF_8);
        }

        if (environment.matchesProfiles(PRODUCTION_PROFILE)) {
            throw new IllegalStateException("app.security.pseudonym-secret is required in production");
        }

        log.warn("No pseudonym secret configured, generating an ephemeral one for this process");
        byte[] ephemeralSecret = new byte[EPHEMERAL_SECRET_BYTES];
        new SecureRandom().nextBytes(ephemeralSecret);

        return ephemeralSecret;
    }
}
