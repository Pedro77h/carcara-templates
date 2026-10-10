package br.com.template.core.modules.auth.repositories;

import br.com.template.core.modules.auth.models.PasswordResetToken;
import br.com.template.core.modules.auth.repositories.projections.PasswordResetTokenStatusProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Query("select t.usedAt as usedAt, t.expiresAt as expiresAt from PasswordResetToken t where t.tokenHash = :tokenHash")
    Optional<PasswordResetTokenStatusProjection> findStatusByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllPendingForUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE (t.usedAt IS NULL AND t.expiresAt < :now) "
            + "OR (t.usedAt IS NOT NULL AND t.createdAt < :usedRetentionLimit)")
    int deleteExhaustedTokens(@Param("now") OffsetDateTime now,
                              @Param("usedRetentionLimit") OffsetDateTime usedRetentionLimit);
}
