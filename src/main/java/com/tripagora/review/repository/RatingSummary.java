package com.tripagora.review.repository;

// 평점 집계 결과: 개수와 평균. 리뷰·템플릿을 엔티티로 불러오지 않고 DB가 계산한다
public record RatingSummary(Long count, Double average) {
}
