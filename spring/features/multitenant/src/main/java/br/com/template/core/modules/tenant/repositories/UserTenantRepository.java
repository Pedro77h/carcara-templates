package br.com.template.core.modules.tenant.repositories;

import br.com.template.core.modules.tenant.models.UserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTenantRepository extends JpaRepository<UserTenant, Long> {
}
