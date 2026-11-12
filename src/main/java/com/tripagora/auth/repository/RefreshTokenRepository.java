package com.tripagora.auth.repository;

import com.tripagora.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository <RefreshToken, Long> {
    Boolean existsByToken(String refreshToken);
    void deleteByUserId(Long userId);
}