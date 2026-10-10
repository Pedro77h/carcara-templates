package br.com.template.core.modules.auth.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auth_attempts")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthAttemptScope scope;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private OffsetDateTime attemptedAt;
}
