package br.com.template.core.config.tenant;

import br.com.template.core.support.CoreTestServer;
import br.com.template.core.support.tenant.TenantNote;
import br.com.template.core.support.tenant.TenantNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Isolamento no nível do Hibernate, sem HTTP: o que vale para jobs e qualquer porta. */
@SpringBootTest
class TenantIsolationTest extends CoreTestServer {

    @Autowired
    private TenantNoteRepository tenantNoteRepository;

    private Long noteOfTenantOne;

    @BeforeEach
    void setUp() {
        TenantContext.runAsRoot(() -> {
            tenantNoteRepository.deleteAll();
            return null;
        });
        noteOfTenantOne = TenantContext.runAs(1L, () -> tenantNoteRepository.save(new TenantNote("um")).getId());
        TenantContext.runAs(2L, () -> tenantNoteRepository.save(new TenantNote("dois")));
    }

    @Test
    @DisplayName("should stamp the current tenant on insert without the code setting it")
    void shouldStampTenantOnInsert() {
        TenantNote note = TenantContext.runAs(1L, () -> tenantNoteRepository.findById(noteOfTenantOne).orElseThrow());

        assertThat(note.getTenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should hide the rows of other tenants from queries and from findById")
    void shouldIsolateTenants() {
        List<TenantNote> visible = TenantContext.runAs(2L, () -> tenantNoteRepository.findAll());
        long count = TenantContext.runAs(2L, () -> tenantNoteRepository.count());

        assertThat(visible).extracting(TenantNote::getText).containsExactly("dois");
        assertThat(count).isEqualTo(1);
        assertThat(TenantContext.runAs(2L, () -> tenantNoteRepository.findById(noteOfTenantOne))).isEmpty();
    }

    @Test
    @DisplayName("should see nothing and write nothing when no tenant was set")
    void shouldFailClosedWithoutTenant() {
        assertThat(tenantNoteRepository.findAll()).isEmpty();
        assertThat(tenantNoteRepository.findById(noteOfTenantOne)).isEmpty();
        assertThatThrownBy(() -> tenantNoteRepository.save(new TenantNote("orfa")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should see every tenant only when running explicitly as root")
    void shouldSeeAllAsRoot() {
        List<TenantNote> all = TenantContext.runAsRoot(() -> tenantNoteRepository.findAll());

        assertThat(all).hasSize(2);
    }
}
