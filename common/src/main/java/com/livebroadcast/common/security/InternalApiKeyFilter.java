package com.livebroadcast.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 서비스 간 내부 호출(/internal/**)을 공유 키로 보호한다.
 * 게이트웨이는 /internal 경로를 외부로 노출하지 않지만, 네트워크 분리에만 의존하지 않기 위해 한 번 더 검사한다.
 * MediaMTX 의 HTTP 인증 훅은 헤더를 직접 넣을 수 없어 key 쿼리 파라미터도 허용한다.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";

    private final byte[] expected;

    public InternalApiKeyFilter(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.internal.api-key 가 설정되지 않았습니다.");
        }
        this.expected = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided == null) {
            provided = request.getParameter("key");
        }
        if (provided == null || !MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"INVALID_INTERNAL_KEY\",\"message\":\"internal only\",\"fieldErrors\":[]}");
            return;
        }
        chain.doFilter(request, response);
    }
}
