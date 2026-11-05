package com.joyeria.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filterChain = mock(FilterChain.class);
    }

    private RateLimitFilter filter(List<String> trustedProxies) {
        return new RateLimitFilter(trustedProxies);
    }

    private MockHttpServletRequest request(String method, String uri, String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setRequestURI(uri);
        req.setRemoteAddr(remoteAddr);
        return req;
    }

    @Test
    void loginRateLimit_AllowsFiveRequests_BlocksSixth() throws Exception {
        RateLimitFilter rateLimit = filter(List.of());

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimit.doFilterInternal(request("POST", "/api/auth/login", "1.1.1.1"), response, filterChain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        rateLimit.doFilterInternal(request("POST", "/api/auth/login", "1.1.1.1"), blocked, filterChain);

        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        assertEquals("0", blocked.getHeader("X-RateLimit-Remaining"));
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    void rateLimit_BucketsArePerIp() throws Exception {
        RateLimitFilter rateLimit = filter(List.of());

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimit.doFilterInternal(request("POST", "/api/auth/login", "2.2.2.2"), response, filterChain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse otherIp = new MockHttpServletResponse();
        rateLimit.doFilterInternal(request("POST", "/api/auth/login", "3.3.3.3"), otherIp, filterChain);
        assertEquals(200, otherIp.getStatus());
    }

    @Test
    void nonApiPath_IsNotRateLimited() throws Exception {
        RateLimitFilter rateLimit = filter(List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimit.doFilterInternal(request("GET", "/index.html", "1.1.1.1"), response, filterChain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("X-RateLimit-Limit"));
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void untrustedProxy_SpoofedXffIsIgnored() throws Exception {
        RateLimitFilter rateLimit = filter(List.of());

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = request("POST", "/api/auth/login", "4.4.4.4");
            req.addHeader("X-Forwarded-For", "10.10.10." + i);
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimit.doFilterInternal(req, response, filterChain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest spoofed = request("POST", "/api/auth/login", "4.4.4.4");
        spoofed.addHeader("X-Forwarded-For", "99.99.99.99");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        rateLimit.doFilterInternal(spoofed, blocked, filterChain);

        assertEquals(429, blocked.getStatus());
    }

    @Test
    void trustedProxy_UsesXffForBucketing() throws Exception {
        RateLimitFilter rateLimit = filter(List.of("10.0.0.1"));

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = request("POST", "/api/auth/login", "10.0.0.1");
            req.addHeader("X-Forwarded-For", "5.5.5.5");
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimit.doFilterInternal(req, response, filterChain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest blocked = request("POST", "/api/auth/login", "10.0.0.1");
        blocked.addHeader("X-Forwarded-For", "5.5.5.5");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        rateLimit.doFilterInternal(blocked, blockedResponse, filterChain);
        assertEquals(429, blockedResponse.getStatus());

        MockHttpServletRequest otherClient = request("POST", "/api/auth/login", "10.0.0.1");
        otherClient.addHeader("X-Forwarded-For", "6.6.6.6");
        MockHttpServletResponse otherResponse = new MockHttpServletResponse();
        rateLimit.doFilterInternal(otherClient, otherResponse, filterChain);
        assertEquals(200, otherResponse.getStatus());
    }

    @Test
    void trustedProxyCidr_MatchesWithinRange() throws Exception {
        RateLimitFilter rateLimit = filter(List.of("10.0.0.0/8"));

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = request("POST", "/api/auth/login", "10.9.9.9");
            req.addHeader("X-Forwarded-For", "7.7.7.7");
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimit.doFilterInternal(req, response, filterChain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest req = request("POST", "/api/auth/login", "10.9.9.9");
        req.addHeader("X-Forwarded-For", "7.7.7.7");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        rateLimit.doFilterInternal(req, blocked, filterChain);
        assertEquals(429, blocked.getStatus());
    }
}
