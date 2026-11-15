package com.tripagora.auth.security;

import com.tripagora.auth.filter.JWTFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JWTFilter jwtFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PublicPaths.PATTERNS).permitAll()
                        .requestMatchers("/api/templates/**").hasRole("GUIDE")
                        // 가이드 프로필은 보기는 누구나, 고치기(소개·사진·포트폴리오)는 가이드만
                        .requestMatchers(HttpMethod.PATCH, "/api/guide-profiles/me/**").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.PUT, "/api/guide-profiles/me/**").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.GET, "/api/trips/**").authenticated()
                        .requestMatchers("/api/trips/**").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.GET, "/api/rooms/{roomId}/notices/**").authenticated()
                        .requestMatchers("/api/rooms/{roomId}/notices/**").hasRole("GUIDE")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}