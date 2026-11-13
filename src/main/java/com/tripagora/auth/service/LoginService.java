package com.tripagora.auth.service;

import com.tripagora.auth.dto.*;
import com.tripagora.auth.exception.SocialAccessTokenInvalidException;
import com.tripagora.auth.exception.SocialJsonParseErrorException;
import com.tripagora.auth.util.JWTUtil;
import com.tripagora.common.image.exception.ImageException;
import com.tripagora.common.image.service.ImageService;
import com.tripagora.user.entity.SocialType;
import com.tripagora.user.entity.User;
import com.tripagora.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
    private final ImageService imageService;
    private final JWTService jwtService;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String KAKAO_USERINFO_URL = "https://kapi.kakao.com/v2/user/me";
    private final String NAVER_USERINFO_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String NO_PROFILE_IMAGE = "";

    @Transactional
    public SocialLoginResponse socialLogin(SocialLoginRequest request, SocialType socialType) {
        String userInfoJson = fetchUserInfo(request.socialAccessToken(), socialType);
        SocialUserInfo userInfo = parseUserInfo(userInfoJson, socialType);

        AtomicBoolean isNewUser = new AtomicBoolean(false);
        User user = saveOrUpdateUser(userInfo.id(), userInfo.profileImageUrl(), socialType, isNewUser);

        String accessToken = jwtUtil.createToken(user.getId(), user.getRole().name(), true);
        String refreshToken = jwtUtil.createToken(user.getId(), user.getRole().name(), false);

        jwtService.deleteRefreshTokenByUserId(user.getId());
        jwtService.saveRefreshToken(user, refreshToken);

        return new SocialLoginResponse(accessToken, refreshToken, isNewUser.get());
    }

    private String fetchUserInfo(String socialAccessToken, SocialType socialType) {
        String url = switch (socialType) {
            case KAKAO -> KAKAO_USERINFO_URL;
            case NAVER -> NAVER_USERINFO_URL;
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(socialAccessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new SocialAccessTokenInvalidException();
        } catch (Exception e) {
            throw new RuntimeException("소셜 로그인 API 요청 실패", e);
        }
    }

    private SocialUserInfo parseUserInfo(String json, SocialType type) {
        try {
            return switch (type) {
                case KAKAO -> {
                    KakaoUserInfo kakao = objectMapper.readValue(json, KakaoUserInfo.class);
                    yield new SocialUserInfo(
                            kakao.getId().toString(),
                            kakao.getKakaoAccount().getProfile().getProfileImageUrl()
                    );
                }
                case NAVER -> {
                    NaverUserInfo naver = objectMapper.readValue(json, NaverUserInfo.class);
                    yield new SocialUserInfo(
                            naver.getResponse().getId(),
                            naver.getResponse().getProfile_image()
                    );
                }
            };
        } catch (JsonProcessingException e) {
            throw new SocialJsonParseErrorException();
        }
    }

    private User saveOrUpdateUser(String socialId, String profileImageUrl, SocialType type, AtomicBoolean isNewUser) {
        Optional<User> userOpt = userRepository.findBySocialIdAndSocialType(socialId, type);

        if (userOpt.isPresent()) {
            isNewUser.set(false);
            return userOpt.get();
        } else {
            String s3ImageUrl = uploadProfileImage(profileImageUrl);

            User newUser = User.builder()
                    .socialId(socialId)
                    .socialType(type)
                    .imageUrl(s3ImageUrl)
                    .build();
            isNewUser.set(true);

            return userRepository.save(newUser);
        }
    }

    // 프로필 사진은 나중에 바꿀 수 있으므로, 소셜 프로필 사진을 가져오지 못해도 가입은 막지 않는다.
    // image_url 컬럼이 NOT NULL이라 사진이 없으면 빈 문자열로 둔다 (앱은 빈 값이면 기본 아바타를 보여 준다)
    private String uploadProfileImage(String profileImageUrl) {
        try {
            String uploadedImageUrl = imageService.uploadImageFromUrl(profileImageUrl);
            return uploadedImageUrl == null ? NO_PROFILE_IMAGE : uploadedImageUrl;
        } catch (ImageException e) {
            log.warn("소셜 프로필 사진을 가져오지 못해 사진 없이 가입합니다: {}", e.getMessage());
            return NO_PROFILE_IMAGE;
        }
    }

    // 로그아웃한 기기로 SOS 알림이 가지 않도록 FCM 토큰도 지운다
    @Transactional
    public void logout(Long userId) {
        jwtService.deleteRefreshTokenByUserId(userId);
        userRepository.findById(userId).ifPresent(user -> user.updateFcmToken(null));
    }
}