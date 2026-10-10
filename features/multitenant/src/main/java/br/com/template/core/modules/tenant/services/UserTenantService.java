package br.com.template.core.modules.tenant.services;

import br.com.template.core.modules.tenant.models.UserTenant;
import br.com.template.core.modules.tenant.repositories.UserTenantRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserTenantService {

    private final UserTenantRepository userTenantRepository;

    public UserTenantService(UserTenantRepository userTenantRepository) {
        this.userTenantRepository = userTenantRepository;
    }

    public Optional<Long> findTenantId(Long userId) {
        return userTenantRepository.findById(userId).map(UserTenant::getTenantId);
    }

    public void assign(Long userId, Long tenantId) {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tenant id must be positive");
        }

        userTenantRepository.save(new UserTenant(userId, tenantId));
    }
}
