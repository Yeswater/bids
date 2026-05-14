package com.yeswater.bids.exec.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    private final String userHeader;
    private final String rolesHeader;
    private final boolean enabled;

    public HeaderAuthenticationFilter(
            @Value("${bids.security.user-header:X-Bids-User}") String userHeader,
            @Value("${bids.security.roles-header:X-Bids-Roles}") String rolesHeader,
            @Value("${bids.security.trusted-header.enabled:false}") boolean enabled
    ) {
        this.userHeader = userHeader;
        this.rolesHeader = rolesHeader;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        String username = request.getHeader(userHeader);
        if (username != null && !username.isBlank()) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(defaultIfBlank(request.getHeader(rolesHeader), "USER").split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .filter(role -> !"ROLE_ADMIN".equals(role))
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(username, null, authorities)
            );
        }
        filterChain.doFilter(request, response);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
