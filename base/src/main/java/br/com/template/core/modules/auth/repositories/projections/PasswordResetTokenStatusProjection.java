package br.com.template.core.modules.auth.repositories.projections;

import java.time.OffsetDateTime;

public interface PasswordResetTokenStatusProjection {
    OffsetDateTime getUsedAt();
    OffsetDateTime getExpiresAt();
}
