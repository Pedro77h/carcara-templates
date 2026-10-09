package br.com.template.core.modules.user.models;

import br.com.template.core.modules.user.converters.RoleListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String phone;

    @Column
    private String password;

    @Column
    @Convert(converter = RoleListConverter.class)
    private List<Role> roles;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role principalRole;

    @Column(nullable = false)
    private boolean active;

    @Column
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime passwordChangedAt;

    @Column
    private OffsetDateTime emailVerifiedAt;

    @Column
    private OffsetDateTime lastLoginAt;

    @Column(nullable = false)
    private int sessionVersion;
}
