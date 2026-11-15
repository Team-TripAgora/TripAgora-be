package com.tripagora.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

// 인증 없이 열어 두는 경로. 보안 설정(permitAll)과 토큰 필터(검사 제외)가 같은 목록을 쓴다
public final class PublicPaths {
    public static final String[] PATTERNS = {
            "/api/auth/login/**",
            "/api/auth/reissue",
            "/actuator/health",
            "/connect/**"
    };

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private PublicPaths() {
    }

    public static boolean matches(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return Arrays.stream(PATTERNS).anyMatch(pattern -> MATCHER.match(pattern, path));
    }
}
