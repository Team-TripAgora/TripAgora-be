package com.tripagora.auth.service;

import com.tripagora.auth.dto.ReissueRequest;
import com.tripagora.auth.dto.ReissueResponse;
import com.tripagora.auth.entity.RefreshToken;
import com.tripagora.auth.exception.RefreshTokenInvalidException;
import com.tripagora.auth.repository.RefreshTokenRepository;
import com.tripagora.auth.util.JWTUtil;
import com.tripagora.auth.util.TokenClaims;
import com.tripagora.auth.util.TokenType;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class JWTService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;

    public ReissueResponse reissue(ReissueRequest reissueRequest) {
        String refreshToken = reissueRequest.refreshToken();

        if(!existsRefreshToken(refreshToken)) {
            throw new RefreshTokenInvalidException();
        }

        long userId = jwtUtil.parse(refreshToken, TokenType.REFRESH)
                .map(TokenClaims::userId)
                .orElseThrow(RefreshTokenInvalidException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        return issueTokens(user);
    }

    public ReissueResponse issueTokensForUser(User user) {
        return issueTokens(user);
    }

    private ReissueResponse issueTokens(User user) {
        String role = user.getRole().name();
        long userId = user.getId();

        String newAccessToken = jwtUtil.createToken(userId, role, TokenType.ACCESS);
        String newRefreshToken = jwtUtil.createToken(userId, role, TokenType.REFRESH);

        deleteRefreshTokenByUserId(userId);
        saveRefreshToken(user, newRefreshToken);

        return new ReissueResponse(newAccessToken, newRefreshToken);
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshEntity = RefreshToken.builder()
                .user(user)
                .token(token)
                .build();

        refreshTokenRepository.save(refreshEntity);
    }

    @Transactional(readOnly = true)
    public Boolean existsRefreshToken(String token) {
        return refreshTokenRepository.existsByToken(token);
    }

    public void deleteRefreshTokenByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}