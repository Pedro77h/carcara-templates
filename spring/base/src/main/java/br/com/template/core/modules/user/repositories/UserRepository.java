package br.com.template.core.modules.user.repositories;

import br.com.template.core.modules.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM User u WHERE u.id = :id")
    int deleteAccountById(@Param("id") Long id);

    Optional<User> findByEmailIgnoreCase(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.active = false, u.updatedAt = :now
            WHERE u.active = true
              AND COALESCE(u.lastLoginAt, u.createdAt) < :threshold
            """)
    int deactivateInactiveSince(@Param("threshold") OffsetDateTime threshold, @Param("now") OffsetDateTime now);

    boolean existsByEmailIgnoreCase(String email);
}
