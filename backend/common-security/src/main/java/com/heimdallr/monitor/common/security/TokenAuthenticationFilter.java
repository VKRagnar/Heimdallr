package com.heimdallr.monitor.common.security;

import com.heimdallr.monitor.common.domain.exception.UnauthorizedException;
import com.heimdallr.monitor.common.observability.RequestId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenPrincipalService tokenPrincipalService;

    public TokenAuthenticationFilter(TokenPrincipalService tokenPrincipalService) {
        this.tokenPrincipalService = tokenPrincipalService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/health".equals(request.getRequestURI()) || request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            CurrentUser currentUser = tokenPrincipalService.authenticate(request.getHeader("Authorization"))
                    .orElseThrow(() -> new UnauthorizedException("Missing or invalid authorization token"));
            RequestUserContext.set(currentUser);
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"code":"UNAUTHORIZED","message":"Missing or invalid authorization token","data":null,"requestId":"%s","timestamp":"%s"}\
                    """.formatted(RequestId.currentOrFallback(), OffsetDateTime.now()));
        } finally {
            RequestUserContext.clear();
        }
    }
}
