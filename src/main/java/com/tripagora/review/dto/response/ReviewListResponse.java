package com.tripagora.review.dto.response;

import java.util.List;

public record ReviewListResponse(
        Double averageRating,
        Integer totalReviewCount,
        List<ReviewResponse> reviews,
        boolean hasNext) {}