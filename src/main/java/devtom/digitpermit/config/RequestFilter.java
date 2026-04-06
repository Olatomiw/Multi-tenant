package devtom.digitpermit.config;

import devtom.digitpermit.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class RequestFilter extends OncePerRequestFilter {

    private static final String TENANT_ID = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_ID);

        if (tenantId == null || tenantId.isBlank()){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing tenant Identifier");
            return;
        }
        try {
            TenantContext.setCurrentTenant(
                    tenantId
            );
            filterChain.doFilter(request, response);
        }finally {
            TenantContext.clearCurrentTenant();
        }

    }
}
