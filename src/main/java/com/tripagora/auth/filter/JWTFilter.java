package com.tripagora.auth.filter;

import com.tripagora.auth.security.PublicPaths;
import com.tripagora.auth.util.JWTUtil;
import com.tripagora.auth.util.TokenClaims;
import com.tripagora.auth.util.TokenType;
import com.tripagora.common.code.AuthErrorCode;
import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.common.response.ApiResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;

    // 로그인·재발급 등 인증이 필요 없는 요청은 토큰을 검사하지 않는다 (만료된 토큰이 붙어 와도 로그인이 되도록)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PublicPaths.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorization.substring(7);

        Optional<TokenClaims> claims = jwtUtil.parse(accessToken, TokenType.ACCESS);
        if (claims.isEmpty()) {
            ResponseCode errorCode = AuthErrorCode.ACCESS_TOKEN_INVALID;
            ApiResponse<?> body = ApiResponse.fail(errorCode);
            ApiResponseWriter.write(response, errorCode.getStatusValue(), body);
            return;
        }

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + claims.get().role()));

        Authentication auth = new UsernamePasswordAuthenticationToken(claims.get().userId(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}