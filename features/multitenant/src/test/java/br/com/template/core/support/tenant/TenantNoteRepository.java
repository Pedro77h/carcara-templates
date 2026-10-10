package br.com.template.core.support.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantNoteRepository extends JpaRepository<TenantNote, Long> {
}
