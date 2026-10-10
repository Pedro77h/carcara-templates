package br.com.template.core.config.tenant;

import br.com.template.core.config.security.jwt.models.AuthenticatedUserPrincipal;
import br.com.template.core.modules.tenant.components.UserTenantComponent;
import br.com.template.core.modules.user.models.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;
import java.util.Optional;

/**
 * Define o tenant da requisição HTTP:
 * <ul>
 *   <li>usuário vinculado a um tenant → o tenant dele; o cabeçalho é ignorado;</li>
 *   <li>{@code ROLE_ADMIN} sem vínculo (admin da plataforma) → {@code X-Tenant-Id}, ou todos sem ele;</li>
 *   <li>qualquer outro caso, inclusive anônimo → {@link TenantContext#NONE}.</li>
 * </ul>
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final UserTenantComponent userTenantComponent;

    public TenantInterceptor(UserTenantComponent userTenantComponent) {
        this.userTenantComponent = userTenantComponent;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        TenantContext.set(resolve(request));

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception exception) {
        TenantContext.clear();
    }

    private Long resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (Objects.isNull(authentication) || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return TenantContext.NONE;
        }

        Optional<Long> ownTenant = userTenantComponent.findTenantId(principal.id());
        if (ownTenant.isPresent()) {
            return ownTenant.get();
        }

        if (!principal.roles().contains(Role.ROLE_ADMIN.name())) {
            return TenantContext.NONE;
        }

        return parseHeader(request.getHeader(TENANT_HEADER));
    }

    private Long parseHeader(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return TenantContext.ROOT;
        }

        try {
            long tenantId = Long.parseLong(value.strip());
            if (tenantId > 0) {
                return tenantId;
            }
        } catch (NumberFormatException ignored) {
            // cai no 400 abaixo
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TENANT_HEADER + " must be a positive number");
    }
}
