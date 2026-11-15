package com.tripagora.auth.util;

// 토큰 종류. 토큰의 type 클레임 값으로 들어간다
public enum TokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String claimValue;

    TokenType(String claimValue) {
        this.claimValue = claimValue;
    }

    public String claimValue() {
        return claimValue;
    }
}
