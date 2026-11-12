package com.tripagora.guideprofile.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.guideprofile.dto.request.GuideBioUpdateRequest;
import com.tripagora.guideprofile.dto.request.GuidePortfolioUpdateRequest;
import com.tripagora.guideprofile.dto.response.GuideBioUpdateResponse;
import com.tripagora.guideprofile.dto.response.GuideImageUpdateResponse;
import com.tripagora.guideprofile.dto.response.GuidePortfolioUpdateResponse;
import com.tripagora.guideprofile.dto.response.GuideProfileDetailResponse;
import com.tripagora.guideprofile.service.GuideProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/guide-profiles")
@RequiredArgsConstructor
public class GuideProfileController {
    private final GuideProfileService guideProfileService;

    @GetMapping("/me")
    public ApiResponse<GuideProfileDetailResponse> getMyGuideProfileDetails(@AuthenticationPrincipal final long userId,
                                                                            @PageableDefault(size = 10) Pageable pageable) {
        GuideProfileDetailResponse response = guideProfileService.getMyGuideProfileDetails(userId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/{guideProfileId}")
    public ApiResponse<GuideProfileDetailResponse> getGuideProfileDetails(@PathVariable final long guideProfileId,
                                                                          @PageableDefault(size = 10) Pageable pageable) {
        GuideProfileDetailResponse response = guideProfileService.getGuideProfileDetails(guideProfileId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/me/bio")
    public ApiResponse<GuideBioUpdateResponse> updateMyBio(@AuthenticationPrincipal final long userId,
                                                           @RequestBody @Valid final GuideBioUpdateRequest request) {
        GuideBioUpdateResponse response = guideProfileService.updateMyBio(userId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GuideImageUpdateResponse> updateMyImageUrl(@AuthenticationPrincipal final long userId,
                                                                  @RequestParam("imageFile") MultipartFile imageFile) {
        GuideImageUpdateResponse response = guideProfileService.updateMyImageUrl(userId, imageFile);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PutMapping("/me/portfolios")
    public ApiResponse<GuidePortfolioUpdateResponse> updateMyPortfolios(@AuthenticationPrincipal final long userId,
                                                                        @RequestBody @Valid final GuidePortfolioUpdateRequest request) {
        GuidePortfolioUpdateResponse response = guideProfileService.updateMyPortfolios(userId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}