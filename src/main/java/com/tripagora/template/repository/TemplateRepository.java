package com.tripagora.template.repository;

import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.review.repository.RatingSummary;
import com.tripagora.template.entity.Template;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    Slice<Template> findByGuideProfileOrderByIdDesc(GuideProfile guideProfile, Pageable pageable);
    // 가이드 평점: 템플릿 리뷰 수의 합, 리뷰가 있는 템플릿 평균들의 평균 (리뷰 없는 템플릿은 CASE가 null이라 AVG에서 빠진다)
    @Query("SELECT new com.tripagora.review.repository.RatingSummary(COALESCE(SUM(t.reviewCount), 0L), " +
            "COALESCE(AVG(CASE WHEN t.reviewCount > 0 THEN t.avgRating END), 0.0)) " +
            "FROM Template t WHERE t.guideProfile = :guideProfile")
    RatingSummary summarizeRatingOf(@Param("guideProfile") GuideProfile guideProfile);
}