package com.tripagora.review.service;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.guideprofile.exception.GuideProfileNotFoundException;
import com.tripagora.guideprofile.repository.GuideProfileRepository;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.review.dto.request.ReviewCreateRequest;
import com.tripagora.review.dto.request.ReviewUpdateRequest;
import com.tripagora.review.dto.response.ReviewListResponse;
import com.tripagora.review.dto.response.ReviewResponse;
import com.tripagora.review.entity.Review;
import com.tripagora.review.exception.AlreadyReviewedException;
import com.tripagora.review.exception.ReviewNotAllowedException;
import com.tripagora.review.exception.ReviewNotFoundException;
import com.tripagora.review.exception.TripNotCompletedException;
import com.tripagora.review.repository.ReviewRepository;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.exception.TripNotFoundException;
import com.tripagora.trip.repository.TripRepository;
import com.tripagora.template.entity.Template;
import com.tripagora.template.exception.TemplateNotFoundException;
import com.tripagora.template.repository.TemplateRepository;
import com.tripagora.user.entity.Role;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TemplateRepository templateRepository;
    private final ParticipationRepository participationRepository;
    private final GuideProfileRepository guideProfileRepository;

    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        User author = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Trip trip = tripRepository.findById(request.sessionId()).orElseThrow(TripNotFoundException::new);
        Template template = trip.getTemplate();
        GuideProfile guideProfile = template.getGuideProfile();

        validateReviewer(author, trip);

        if (trip.getStatus() != TripStatus.COMPLETED) {
             throw new TripNotCompletedException();
        }

        if (reviewRepository.existsByAuthor_IdAndTrip_Id(userId, request.sessionId())) {
            throw new AlreadyReviewedException();
        }

        Review review = Review.builder()
                .author(author)
                .trip(trip)
                .template(template)
                .guideProfile(guideProfile)
                .content(request.content())
                .rating(request.rating())
                .build();
        Review savedReview = reviewRepository.save(review);

        updateTemplateRating(template);
        updateGuideProfileRating(guideProfile);

        return new ReviewResponse(
                template.getTitle(),
                savedReview.getId(),
                savedReview.getAuthor().getNickname(),
                savedReview.getAuthor().getImageUrl(),
                savedReview.getContent(),
                savedReview.getRating(),
                savedReview.getCreatedAt()
                );
    }

    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = findReviewAndVerifyOwner(userId, reviewId);

        review.update(request.content(), request.rating());

        updateTemplateRating(review.getTemplate());
        updateGuideProfileRating(review.getGuideProfile());

        return new ReviewResponse(
                review.getTemplate().getTitle(),
                review.getId(),
                review.getAuthor().getNickname(),
                review.getAuthor().getImageUrl(),
                review.getContent(),
                review.getRating(),
                review.getCreatedAt()
        );
    }

    public void deleteReview(Long userId, Long reviewId) {
        Review review = findReviewAndVerifyOwner(userId, reviewId);
        Template template = review.getTemplate();
        GuideProfile guideProfile = review.getGuideProfile();

        reviewRepository.delete(review);

        updateTemplateRating(template);
        updateGuideProfileRating(guideProfile);
    }

    @Transactional(readOnly = true)
    public ReviewListResponse getGuideReviews(Long guideProfileId, Pageable pageable) {
        GuideProfile guideProfile = guideProfileRepository.findById(guideProfileId).orElseThrow(GuideProfileNotFoundException::new);
        Slice<Review> reviewSlice = reviewRepository.findByGuideProfile(guideProfile, pageable);

        List<ReviewResponse> reviews = reviewSlice.getContent().stream()
                .map(review -> new ReviewResponse(
                        review.getTemplate().getTitle(),
                        review.getId(),
                        review.getAuthor().getNickname(),
                        review.getAuthor().getImageUrl(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt()
                ))
                .toList();

        return new ReviewListResponse(
                guideProfile.getTotalAvgRating(),
                guideProfile.getTotalReviewCount(),
                reviews,
                reviewSlice.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public ReviewListResponse getTemplateReviews(Long templateId, Pageable pageable) {
        Template template = templateRepository.findById(templateId).orElseThrow(TemplateNotFoundException::new);

        Slice<Review> reviewSlice = reviewRepository.findByTemplate(template, pageable);

        List<ReviewResponse> reviews = reviewSlice.getContent().stream()
                .map(review -> new ReviewResponse(
                        template.getTitle(),
                        review.getId(),
                        review.getAuthor().getNickname(),
                        review.getAuthor().getImageUrl(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt()
                ))
                .toList();

        return new ReviewListResponse(
                template.getAvgRating(),
                template.getReviewCount(),
                reviews,
                reviewSlice.hasNext()
        );
    }

    private void updateTemplateRating(Template template) {
        List<Review> reviews = reviewRepository.findByTemplate(template);
        int reviewCount = reviews.size();

        double avgRating = reviews.stream()
                .map(Review::getRating)
                .filter(Objects::nonNull) // null인 평점은 집계에서 제외
                .mapToInt(Integer::intValue) // null이 걸러진 후에 int로 변환
                .average()
                .orElse(0.0);

        template.updateRating(avgRating, reviewCount);
    }

    private void updateGuideProfileRating(GuideProfile guideProfile) {
        List<Template> templates = templateRepository.findByGuideProfile(guideProfile);

        int totalReviewCount = templates.stream()
                .map(Template::getReviewCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        double avgOfTemplateAvgs = templates.stream()
                .filter(t -> t.getReviewCount() != null && t.getReviewCount() > 0)
                .map(Template::getAvgRating)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        guideProfile.updateRating(avgOfTemplateAvgs, totalReviewCount);
    }

    private void validateReviewer(User user, Trip trip) {
        participationRepository.findByUser_IdAndTrip_Id(user.getId(), trip.getId())
                .filter(p -> p.getRole() == Role.TRAVELER)
                .orElseThrow(ReviewNotAllowedException::new);
    }

    private Review findReviewAndVerifyOwner(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(ReviewNotFoundException::new);

        if (!Objects.equals(review.getAuthor().getId(), userId)) {
            throw new AccessDeniedException();
        }

        return review;
    }
}