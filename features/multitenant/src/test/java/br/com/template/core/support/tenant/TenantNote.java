package br.com.template.core.support.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TenantId;

/** Entidade só de teste, no formato que uma entidade de domínio multi-tenant deve ter. */
@Entity
@Table(name = "tenant_notes")
@Getter
@NoArgsConstructor
public class TenantNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String text;

    public TenantNote(String text) {
        this.text = text;
    }
}
