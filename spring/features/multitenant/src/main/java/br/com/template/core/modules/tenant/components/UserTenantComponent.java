package br.com.template.core.modules.tenant.components;

import br.com.template.core.modules.tenant.services.UserTenantService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserTenantComponent {

    private final UserTenantService userTenantService;

    public UserTenantComponent(UserTenantService userTenantService) {
        this.userTenantService = userTenantService;
    }

    public Optional<Long> findTenantId(Long userId) {
        return userTenantService.findTenantId(userId);
    }

    /** Chamado pelo fluxo de domínio que cria o vínculo (ex.: cadastro de membro da agência). */
    public void assign(Long userId, Long tenantId) {
        userTenantService.assign(userId, tenantId);
    }
}
