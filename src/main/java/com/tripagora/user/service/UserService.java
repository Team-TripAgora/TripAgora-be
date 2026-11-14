package com.tripagora.user.service;

import com.tripagora.auth.repository.RefreshTokenRepository;
import com.tripagora.common.image.event.ImageDeletionEvent;
import com.tripagora.common.image.service.ImageService;
import com.tripagora.location.repository.UserLocationRepository;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.tag.entity.Tag;
import com.tripagora.tag.exception.InvalidTagSelectionException;
import com.tripagora.tag.exception.MinimumTagsRequiredException;
import com.tripagora.tag.repository.TagRepository;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.dto.request.NicknameUpdateRequest;
import com.tripagora.user.dto.response.NicknameUpdateResponse;
import com.tripagora.user.dto.request.UserTagUpdateRequest;
import com.tripagora.user.dto.response.ProfileImageUpdateResponse;
import com.tripagora.user.dto.response.UserInfoResponse;
import com.tripagora.user.dto.response.UserTagUpdateResponse;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.DuplicateNicknameException;
import com.tripagora.user.exception.InvalidNicknameFormatException;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.exception.WithdrawalNotAllowedException;
import com.tripagora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    // 이 상태의 여행에 가이드나 참가자로 있으면 탈퇴할 수 없다 (가이드는 여행을 열 때 참가 기록이 생긴다)
    private static final List<TripStatus> ACTIVE_TRIP_STATUSES = List.of(
            TripStatus.RECRUITING,
            TripStatus.RECRUITMENT_CLOSED,
            TripStatus.IN_PROGRESS
    );

    private final ImageService imageService;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ParticipationRepository participationRepository;
    private final UserLocationRepository userLocationRepository;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        String nickname = user.getNickname();
        String role = user.getRole().toString();
        String imageUrl = user.getImageUrl();

        List<Long> tagIds = user.getUserTags().stream()
                .map(userTag -> userTag.getTag().getId())
                .toList();

        return new UserInfoResponse(nickname, role, imageUrl, tagIds);
    }

    @Transactional
    public void updateFcmToken(long userId, String token) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // 같은 기기에서 다른 계정으로 로그인한 경우: 토큰은 마지막에 등록한 계정의 것으로 두고 이전 계정에서는 지운다
        userRepository.findAllByFcmTokenAndIdNot(token, userId)
                .forEach(previousOwner -> previousOwner.updateFcmToken(null));

        user.updateFcmToken(token);
    }

    @Transactional
    public void withdrawUser(long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (participationRepository.existsByUser_IdAndTrip_StatusIn(userId, ACTIVE_TRIP_STATUSES)) {
            throw new WithdrawalNotAllowedException();
        }

        // 하드 삭제하면 남의 참가·리뷰·채팅 이력까지 지워지거나 외래 키에 막힌다. 행은 남기고 익명화한다
        refreshTokenRepository.deleteByUserId(userId);
        userLocationRepository.deleteByUser_Id(userId);
        user.withdraw();
    }

    @Transactional
    public NicknameUpdateResponse updateNickname(long userId, NicknameUpdateRequest request) {
        String nickname = request.nickname();

        if (nickname.matches(".*[\\s\\p{Cntrl}].*")) {
            throw new InvalidNicknameFormatException();
        }

        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
            throw new DuplicateNicknameException();
        }

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.updateNickname(nickname);

        // 중복 확인 후 다른 요청이 같은 닉네임을 먼저 저장한 경우 unique 제약 위반을 중복 예외로 바꾼다
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateNicknameException();
        }

        return new NicknameUpdateResponse(user.getNickname());
    }

    @Transactional
    public UserTagUpdateResponse updateTags(long userId, UserTagUpdateRequest request) {
        List<Long> tagIds = request.tagIds();

        if (tagIds == null || tagIds.size() < 3) {
            throw new MinimumTagsRequiredException();
        }

        List<Tag> newTags = tagRepository.findAllById(tagIds);
        if (newTags.size() != tagIds.size()) {
            throw new InvalidTagSelectionException();
        }

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        user.replaceTags(newTags);

        List<Long> newTagIds = newTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toList());

        return new UserTagUpdateResponse(newTagIds);
    }

    // S3 업로드 동안 DB 트랜잭션을 열어 두지 않는다: 새 사진 업로드(트랜잭션 밖) → DB 저장(짧은 트랜잭션).
    // 옛 사진은 DB가 커밋된 뒤에만 지우고, 저장이 실패하면 방금 올린 새 사진을 지운다
    public ProfileImageUpdateResponse updateProfileImage(long userId, MultipartFile imageFile) {
        String newImageUrl = imageService.uploadImage(imageFile);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
                String oldImageUrl = user.getImageUrl();
                user.updateImageUrl(newImageUrl);
                eventPublisher.publishEvent(ImageDeletionEvent.of(oldImageUrl));
            });
        } catch (RuntimeException e) {
            imageService.deleteImagesQuietly(List.of(newImageUrl));
            throw e;
        }

        return new ProfileImageUpdateResponse(newImageUrl);
    }
}