package com.tripagora.review.repository;

import com.tripagora.guideProfile.entity.GuideProfile;
import com.tripagora.review.entity.Review;
import com.tripagora.template.entity.Template;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByAuthor_IdAndSession_Id(Long authorId, Long sessionId);
    Optional<Review> findByAuthor_IdAndSession_Id(Long authorId, Long sessionId);
    List<Review> findByTemplate(Template template);
    Slice<Review> findByTemplate(Template template, Pageable pageable);
    Slice<Review> findByGuideProfile(GuideProfile guideProfile, Pageable pageable);
}