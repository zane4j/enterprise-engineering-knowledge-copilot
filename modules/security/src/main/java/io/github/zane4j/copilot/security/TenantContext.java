package io.github.zane4j.copilot.security;

import io.github.zane4j.copilot.common.DomainException;
import java.util.Optional;

/**
 * Request-scoped tenant holder for synchronous HTTP processing.
 * Background jobs must carry the tenant id in their message payload instead of relying on ThreadLocal.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantPrincipal> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Scope open(TenantPrincipal principal) {
        TenantPrincipal previous = CURRENT.get();
        CURRENT.set(principal);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    public static TenantPrincipal requireCurrent() {
        return Optional.ofNullable(CURRENT.get())
                .orElseThrow(() -> new DomainException("TENANT_CONTEXT_MISSING", "Tenant context is required"));
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
