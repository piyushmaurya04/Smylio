package Tenant.Owner.Management.TOM.Security;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthOriginValidatorFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthOriginValidatorFilter.class);

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    private final Set<String> allowedOrigins;

    public AuthOriginValidatorFilter(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOriginsConfig) {
        if (allowedOriginsConfig != null && !allowedOriginsConfig.isBlank()) {
            this.allowedOrigins = Arrays.stream(allowedOriginsConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(AuthOriginValidatorFilter::normalizeOrigin)
                    .collect(Collectors.toSet());
        } else {
            this.allowedOrigins = Collections.emptySet();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod()) || !isProtectedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String secFetchSite = request.getHeader("Sec-Fetch-Site");
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        if (origin != null && !origin.isBlank()) {
            if (!isAllowedOrigin(request, origin)) {
                reject(request, response, "Origin '" + origin + "' is not permitted");
                return;
            }
        } else if (referer != null && !referer.isBlank()) {
            String refererOrigin = extractOriginFromReferer(referer);
            if (refererOrigin == null || !isAllowedOrigin(request, refererOrigin)) {
                reject(request, response, "Referer '" + referer + "' is not permitted");
                return;
            }
        } else if ("cross-site".equalsIgnoreCase(secFetchSite)) {
            reject(request, response, "Cross-site request blocked via Sec-Fetch-Site");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedPath(String uri) {
        if (uri == null) {
            return false;
        }
        String normalized = uri.endsWith("/") && uri.length() > 1
                ? uri.substring(0, uri.length() - 1)
                : uri;
        return PROTECTED_PATHS.contains(normalized);
    }

    private boolean isAllowedOrigin(HttpServletRequest request, String originHeader) {
        if (originHeader == null || originHeader.isBlank()) {
            return false;
        }
        String normalizedOrigin = normalizeOrigin(originHeader);
        if (allowedOrigins.contains(normalizedOrigin)) {
            return true;
        }

        // Validate against request's same-origin only when Host header is consistent with the server name
        String serverName = request.getServerName();
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank() && serverName != null && !serverName.isBlank()) {
            String hostNameOnly = hostHeader.contains(":") ? hostHeader.substring(0, hostHeader.indexOf(":")) : hostHeader;
            if (hostNameOnly.equalsIgnoreCase(serverName.trim())) {
                String scheme = request.getScheme();
                String hostOrigin = normalizeOrigin(scheme + "://" + hostHeader);
                if (hostOrigin.equalsIgnoreCase(normalizedOrigin)) {
                    return true;
                }
            }
        }

        int port = request.getServerPort();
        String scheme = request.getScheme();
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443)
                || port <= 0;
        String constructed = scheme + "://" + request.getServerName() + (isDefaultPort ? "" : ":" + port);
        return normalizeOrigin(constructed).equalsIgnoreCase(normalizedOrigin);
    }

    private String extractOriginFromReferer(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getScheme() != null && uri.getHost() != null) {
                int port = uri.getPort();
                boolean isDefaultPort = ("http".equalsIgnoreCase(uri.getScheme()) && port == 80)
                        || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443)
                        || port == -1;
                return normalizeOrigin(uri.getScheme() + "://" + uri.getHost() + (isDefaultPort ? "" : ":" + port));
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    static String normalizeOrigin(String origin) {
        if (origin == null) {
            return "";
        }
        String trimmed = origin.trim().toLowerCase(Locale.ROOT);
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String reason) throws IOException {
        logger.warn("CSRF Origin validation blocked request: method={} path={} reason={}",
                request.getMethod(), request.getRequestURI(), reason);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":403,\"message\":\"Forbidden: invalid request origin\"}");
    }
}

