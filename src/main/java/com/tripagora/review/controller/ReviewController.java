package com.tripagora.review.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.review.dto.request.ReviewCreateRequest;
import com.tripagora.review.dto.request.ReviewUpdateRequest;
import com.tripagora.review.dto.response.ReviewListResponse;
import com.tripagora.review.dto.response.ReviewResponse;
import com.tripagora.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewResponse> createReview(@AuthenticationPrincipal final long userId,
                                                    @RequestBody @Valid final ReviewCreateRequest request) {
        ReviewResponse response = reviewService.createReview(userId, request);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @PutMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(@AuthenticationPrincipal final long userId,
                                                    @PathVariable final long reviewId,
                                                    @RequestBody @Valid final ReviewUpdateRequest request) {
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(@AuthenticationPrincipal final long userId,
                                          @PathVariable final long reviewId) {
        reviewService.deleteReview(userId, reviewId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @GetMapping("/guide/{guideProfileId}")
    public ApiResponse<ReviewListResponse> getGuideReviews(@PathVariable final long guideProfileId,
                                                           @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable) {
        ReviewListResponse response = reviewService.getGuideReviews(guideProfileId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/template/{templateId}")
    public ApiResponse<ReviewListResponse> getTemplateReviews(@PathVariable final long templateId,
                                                              @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable) {
        ReviewListResponse response = reviewService.getTemplateReviews(templateId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}