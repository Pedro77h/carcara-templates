package br.com.template.core.config.tenant;

import br.com.template.core.config.security.jwt.services.JwtService;
import br.com.template.core.modules.tenant.components.UserTenantComponent;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.repositories.UserRepository;
import br.com.template.core.support.CoreTestServer;
import br.com.template.core.support.tenant.TenantNote;
import br.com.template.core.support.tenant.TenantNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O caminho inteiro de uma requisição: JWT → interceptor → transação → Hibernate. É este teste
 * que pega um tenant definido tarde demais (depois de a sessão abrir) ou num lugar onde não chega.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TenantInterceptorFlowTest extends CoreTestServer {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserTenantComponent userTenantComponent;
    @Autowired
    private TenantNoteRepository tenantNoteRepository;
    @Autowired
    private JwtService jwtService;

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

    private String tokenFor(Role role, Long tenantId) {
        User user = userRepository.save(User.builder()
                .fullName("Pessoa Teste")
                .email("pessoa" + System.nanoTime() + "@example.com")
                .password("hash")
                .principalRole(role)
                .roles(List.of(role))
                .active(true)
                .build());
        if (tenantId != null) {
            userTenantComponent.assign(user.getId(), tenantId);
        }

        return jwtService.generateAccessToken(user);
    }

    private MockHttpServletRequestBuilder as(String token, String path) {
        return get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    @Test
    @DisplayName("should show a tenant member only the rows of their tenant, header or not")
    void shouldScopeMemberToOwnTenant() throws Exception {
        String member = tokenFor(Role.ROLE_USER, 1L);

        mockMvc.perform(as(member, "/api/test/tenant-notes").header(TenantInterceptor.TENANT_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("um"));
    }

    @Test
    @DisplayName("should not let a member of another tenant read a row by id")
    void shouldHideOtherTenantRowById() throws Exception {
        String otherMember = tokenFor(Role.ROLE_USER, 2L);

        mockMvc.perform(as(otherMember, "/api/test/tenant-notes/" + noteOfTenantOne))
                .andExpect(content().string("not-found"));
    }

    @Test
    @DisplayName("should even scope a tenant admin to their own tenant")
    void shouldScopeTenantAdmin() throws Exception {
        String tenantAdmin = tokenFor(Role.ROLE_ADMIN, 2L);

        mockMvc.perform(as(tenantAdmin, "/api/test/tenant-notes").header(TenantInterceptor.TENANT_HEADER, "1"))
                .andExpect(jsonPath("$[0]").value("dois"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("should let a platform admin pick a tenant by header, or see all without it")
    void shouldLetPlatformAdminChoose() throws Exception {
        String platformAdmin = tokenFor(Role.ROLE_ADMIN, null);

        mockMvc.perform(as(platformAdmin, "/api/test/tenant-notes").header(TenantInterceptor.TENANT_HEADER, "2"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("dois"));
        mockMvc.perform(as(platformAdmin, "/api/test/tenant-notes"))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(as(platformAdmin, "/api/test/tenant-notes").header(TenantInterceptor.TENANT_HEADER, "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should show nothing to a user bound to no tenant who is not an admin")
    void shouldFailClosedForUnboundUser() throws Exception {
        String loose = tokenFor(Role.ROLE_USER, null);

        mockMvc.perform(as(loose, "/api/test/tenant-notes").header(TenantInterceptor.TENANT_HEADER, "1"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("should not leak the tenant of a request into the thread afterwards")
    void shouldClearTenantAfterRequest() throws Exception {
        mockMvc.perform(as(tokenFor(Role.ROLE_USER, 1L), "/api/test/tenant-notes")).andExpect(status().isOk());

        assertThat(TenantContext.current()).isEqualTo(TenantContext.NONE);
    }
}
