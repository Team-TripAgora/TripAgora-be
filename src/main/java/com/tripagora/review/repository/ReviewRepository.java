package com.tripagora.review.repository;

import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.review.entity.Review;
import com.tripagora.template.entity.Template;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByAuthor_IdAndTrip_Id(Long authorId, Long tripId);
    Optional<Review> findByAuthor_IdAndTrip_Id(Long authorId, Long tripId);
    // 한 사람이 여러 여행에 쓴 리뷰를 한 번에 읽는다 (완료 여행 목록의 '내 리뷰')
    List<Review> findByAuthor_IdAndTrip_IdIn(Long authorId, Collection<Long> tripIds);
    // 템플릿 평점: 리뷰 수와 평균. 정수 평점의 AVG는 MySQL이 소수 4자리로 자르므로 실수로 바꿔 평균을 낸다
    @Query("SELECT new com.tripagora.review.repository.RatingSummary(COUNT(r), COALESCE(AVG(CAST(r.rating AS Double)), 0.0)) " +
            "FROM Review r WHERE r.template = :template")
    RatingSummary summarizeRatingOf(@Param("template") Template template);
    Slice<Review> findByTemplate(Template template, Pageable pageable);
    Slice<Review> findByGuideProfile(GuideProfile guideProfile, Pageable pageable);
}