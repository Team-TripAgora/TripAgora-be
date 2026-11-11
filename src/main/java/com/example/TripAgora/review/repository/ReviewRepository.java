package com.example.TripAgora.review.repository;

import com.example.TripAgora.guideProfile.entity.GuideProfile;
import com.example.TripAgora.review.entity.Review;
import com.example.TripAgora.template.entity.Template;
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