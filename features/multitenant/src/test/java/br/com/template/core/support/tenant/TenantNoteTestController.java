package br.com.template.core.support.tenant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Só de teste: atravessa interceptor → transação → Hibernate, o caminho real de uma requisição. */
@RestController
class TenantNoteTestController {

    private final TenantNoteRepository tenantNoteRepository;

    TenantNoteTestController(TenantNoteRepository tenantNoteRepository) {
        this.tenantNoteRepository = tenantNoteRepository;
    }

    @GetMapping("/api/test/tenant-notes")
    List<String> list() {
        return tenantNoteRepository.findAll().stream().map(TenantNote::getText).sorted().toList();
    }

    @GetMapping("/api/test/tenant-notes/{id}")
    String find(@PathVariable Long id) {
        return tenantNoteRepository.findById(id).map(TenantNote::getText).orElse("not-found");
    }
}
