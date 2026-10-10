package br.com.template.core.config.tenant;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Tenant da thread atual. Quem define é o {@link TenantInterceptor} (HTTP) ou
 * {@link #runAsRoot(Supplier)} (jobs). Precisa estar definido <b>antes</b> de a transação abrir:
 * o Hibernate lê o tenant uma vez, quando abre a sessão.
 *
 * <p>Sem nada definido, vale {@link #NONE}: nenhuma linha de tenant aparece e nenhuma pode ser
 * gravada. Porta nova que esqueça de definir o tenant falha fechada, nunca aberta.
 */
public final class TenantContext {

    /** Não enxerga nenhum tenant. A coluna {@code tenant_id} tem {@code CHECK (tenant_id > 0)}. */
    public static final Long NONE = 0L;

    /** Enxerga todos os tenants — admin da plataforma sem cabeçalho, ou job explícito. */
    public static final Long ROOT = -1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long current() {
        return Objects.requireNonNullElse(CURRENT.get(), NONE);
    }

    static void set(Long tenantId) {
        CURRENT.set(tenantId);
    }

    static void clear() {
        CURRENT.remove();
    }

    public static <T> T runAs(Long tenantId, Supplier<T> action) {
        Long previous = CURRENT.get();
        CURRENT.set(tenantId);

        try {
            return action.get();
        } finally {
            if (Objects.isNull(previous)) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static <T> T runAsRoot(Supplier<T> action) {
        return runAs(ROOT, action);
    }
}
