package com.tripagora.user.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.guideprofile.service.GuideProfileService;
import com.tripagora.user.dto.request.NicknameUpdateRequest;
import com.tripagora.user.dto.request.UserTagUpdateRequest;
import com.tripagora.user.dto.response.*;
import com.tripagora.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final GuideProfileService guideProfileService;

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getUserInfo(@AuthenticationPrincipal final long userId) {
        UserInfoResponse response = userService.getUserInfo(userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdrawUser(@AuthenticationPrincipal final long userId) {
        userService.withdrawUser(userId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<NicknameUpdateResponse> updateNickname(@AuthenticationPrincipal final long userId,
                                                              @RequestBody @Valid final NicknameUpdateRequest request) {
        NicknameUpdateResponse response = userService.updateNickname(userId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/me/tags")
    public ApiResponse<UserTagUpdateResponse> updateTags(@AuthenticationPrincipal final long userId,
                                                         @RequestBody @Valid final UserTagUpdateRequest request) {
        UserTagUpdateResponse response = userService.updateTags(userId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileImageUpdateResponse> updateProfileImage(@AuthenticationPrincipal final long userId,
                                                                      @RequestParam("imageFile") MultipartFile imageFile) {
        ProfileImageUpdateResponse response = userService.updateProfileImage(userId, imageFile);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/me/switch-to-guide")
    public ApiResponse<GuideSwitchResponse> switchToGuide(@AuthenticationPrincipal final long userId) {
        GuideSwitchResponse response = guideProfileService.switchToGuide(userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/me/switch-to-traveler")
    public ApiResponse<TravelerSwitchResponse> switchToTraveler(@AuthenticationPrincipal final long userId) {
        TravelerSwitchResponse response = guideProfileService.switchToTraveler(userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}