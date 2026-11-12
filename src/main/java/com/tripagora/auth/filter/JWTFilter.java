package com.tripagora.auth.filter;

import com.tripagora.auth.util.JWTUtil;
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

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorization.substring(7);

        if (!jwtUtil.isValid(accessToken, true)) {
            ResponseCode errorCode = AuthErrorCode.ACCESS_TOKEN_INVALID;
            ApiResponse<?> body = ApiResponse.fail(errorCode);
            ApiResponseWriter.write(response, errorCode.getStatusValue(), body);
            return;
        }

        Long userId = jwtUtil.getUserId(accessToken);
        String role = jwtUtil.getRole(accessToken);
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));

        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}