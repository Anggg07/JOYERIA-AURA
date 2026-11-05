package com.joyeria.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joyeria.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long DEFAULT_LIMIT = 120;
    private static final long DEFAULT_WINDOW_SECONDS = 60;
    private static final long CLEANUP_INTERVAL_MS = 60_000L;

    private static final Map<String, Rule> SPECIFIC_RULES = Map.of(
            "POST|/api/auth/login", new Rule(5, 60),
            "POST|/api/auth/register", new Rule(5, 60),
            "POST|/api/auth/forgot-password", new Rule(3, 600),
            "POST|/api/auth/reset-password", new Rule(5, 3600),
            "POST|/api/coupons/validate", new Rule(20, 60),
            "POST|/api/orders", new Rule(10, 60),
            "POST|/api/ai/assistant", new Rule(20, 60),
            "POST|/api/admin/ai/assistant", new Rule(40, 60),
            "POST|/api/gift-finder/recommend", new Rule(20, 60)
    );

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> trustedProxies;

    private volatile long lastCleanupMs = System.currentTimeMillis();

    public RateLimitFilter(
            @Value("${app.security.trusted-proxies:}") List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);

        String routeKey = request.getMethod() + "|" + path;
        Rule rule = SPECIFIC_RULES.getOrDefault(routeKey, new Rule(DEFAULT_LIMIT, DEFAULT_WINDOW_SECONDS));
        String ip = resolveClientIp(request);
        String bucketKey = ip + "|" + routeKey;

        long windowMillis = rule.seconds() * 1000L;
        Window window = windows.compute(bucketKey, (key, existing) -> {
            if (existing == null || now > existing.end()) {
                return new Window(1, now + windowMillis);
            }
            return new Window(existing.count() + 1, existing.end());
        });

        long retryAfterSeconds = Math.max(0, (window.end() - now) / 1000 + 1);
        long remaining = Math.max(0, rule.limit() - window.count());

        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(window.end()));

        if (window.count() > rule.limit()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("Demasiadas solicitudes. Intenta de nuevo en " + retryAfterSeconds + " segundos."));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String addr) {
        for (String entry : trustedProxies) {
            String proxy = entry.trim();
            if (proxy.isEmpty()) {
                continue;
            }
            if (proxy.contains("/")) {
                if (matchesCidr(addr, proxy)) {
                    return true;
                }
            } else if (proxy.equals(addr)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCidr(String addr, String cidr) {
        try {
            String[] parts = cidr.split("/");
            long network = ipv4ToLong(parts[0].trim());
            int prefix = Integer.parseInt(parts[1].trim());
            long addrLong = ipv4ToLong(addr);
            if (network < 0 || addrLong < 0 || prefix < 0 || prefix > 32) {
                return false;
            }
            long mask = prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            return (network & mask) == (addrLong & mask);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private long ipv4ToLong(String ip) {
        String[] octets = ip.trim().split("\\.");
        if (octets.length != 4) {
            return -1;
        }
        long value = 0;
        for (String octet : octets) {
            int b = Integer.parseInt(octet);
            if (b < 0 || b > 255) {
                return -1;
            }
            value = (value << 8) | b;
        }
        return value;
    }

    private void cleanupIfNeeded(long now) {
        if (now - lastCleanupMs >= CLEANUP_INTERVAL_MS) {
            lastCleanupMs = now;
            windows.entrySet().removeIf(entry -> now > entry.getValue().end() + 600_000L);
        }
    }

    private record Window(long count, long end) {}

    private record Rule(long limit, long seconds) {}
}