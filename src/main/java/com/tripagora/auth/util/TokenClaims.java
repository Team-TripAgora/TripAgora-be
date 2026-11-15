package com.tripagora.auth.util;

// 검증을 통과한 토큰에서 꺼낸 값
public record TokenClaims(Long userId, String role) {
}
