package br.com.template.core.modules.auth.repositories;

import br.com.template.core.modules.auth.models.AuthAttempt;
import br.com.template.core.modules.auth.models.AuthAttemptScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, Long> {

    @Query("""
            SELECT COUNT(a) FROM AuthAttempt a
            WHERE a.scope = :scope
              AND a.identifier = :identifier
              AND a.attemptedAt >= :since
            """)
    long countSince(@Param("scope") AuthAttemptScope scope,
                    @Param("identifier") String identifier,
                    @Param("since") OffsetDateTime since);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM AuthAttempt a
            WHERE a.scope = :scope
              AND a.identifier = :identifier
            """)
    int deleteByScopeAndIdentifier(@Param("scope") AuthAttemptScope scope, @Param("identifier") String identifier);

    @Modifying
    @Query("DELETE FROM AuthAttempt a WHERE a.attemptedAt < :threshold")
    int deleteAttemptedBefore(@Param("threshold") OffsetDateTime threshold);
}
