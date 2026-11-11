package com.example.TripAgora.user.service;

import com.example.TripAgora.auth.repository.RefreshTokenRepository;
import com.example.TripAgora.common.image.service.ImageService;
import com.example.TripAgora.tag.entity.Tag;
import com.example.TripAgora.tag.entity.UserTag;
import com.example.TripAgora.tag.exception.InvalidTagSelectionException;
import com.example.TripAgora.tag.exception.MinimumTagsRequiredException;
import com.example.TripAgora.tag.repository.TagRepository;
import com.example.TripAgora.tag.repository.UserTagRepository;
import com.example.TripAgora.user.dto.request.NicknameUpdateRequest;
import com.example.TripAgora.user.dto.response.NicknameUpdateResponse;
import com.example.TripAgora.user.dto.request.UserTagUpdateRequest;
import com.example.TripAgora.user.dto.response.ProfileImageUpdateResponse;
import com.example.TripAgora.user.dto.response.UserInfoResponse;
import com.example.TripAgora.user.dto.response.UserTagUpdateResponse;
import com.example.TripAgora.user.entity.User;
import com.example.TripAgora.user.exception.DuplicateNicknameException;
import com.example.TripAgora.user.exception.InvalidNicknameFormatException;
import com.example.TripAgora.user.exception.UserNotFoundException;
import com.example.TripAgora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final UserTagRepository userTagRepository;
    private final RefreshTokenRepository refreshTokenRepository;

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
    public void withdrawUser(long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        refreshTokenRepository.deleteByUserId(userId);
        userRepository.delete(user);
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

        userTagRepository.deleteByUser(user);
        List<UserTag> newUserTags = newTags.stream()
                .map(tag -> UserTag.builder()
                        .user(user)
                        .tag(tag)
                        .build())
                .toList();
        userTagRepository.saveAll(newUserTags);

        List<Long> newTagIds = newTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toList());

        return new UserTagUpdateResponse(newTagIds);
    }

    @Transactional
    public ProfileImageUpdateResponse updateProfileImage(long userId, MultipartFile imageFile) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        String oldImageUrl = user.getImageUrl();
        String newImageUrl = imageService.updateImage(oldImageUrl, imageFile);

        user.updateImageUrl(newImageUrl);

        return new ProfileImageUpdateResponse(newImageUrl);
    }
}