package br.com.template.core.modules.tenant.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vínculo usuário → tenant (um tenant por usuário). Não é {@code @TenantId}: precisa ser lido
 * antes de o tenant da requisição existir. Sem linha aqui, o usuário é da plataforma.
 */
@Entity
@Table(name = "user_tenants")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserTenant {

    @Id
    private Long userId;

    @Column(nullable = false)
    private Long tenantId;
}
