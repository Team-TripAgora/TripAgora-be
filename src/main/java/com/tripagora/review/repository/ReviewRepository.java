package com.tripagora.review.repository;

import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.review.entity.Review;
import com.tripagora.template.entity.Template;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<Review> findByTemplate(Template template);
    Slice<Review> findByTemplate(Template template, Pageable pageable);
    Slice<Review> findByGuideProfile(GuideProfile guideProfile, Pageable pageable);
}