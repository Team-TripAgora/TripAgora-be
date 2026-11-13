package com.tripagora.trip.service;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.guideprofile.exception.GuideProfileNotFoundException;
import com.tripagora.participation.entity.Participation;
import com.tripagora.participation.exception.ParticipationNotFoundException;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.review.entity.Review;
import com.tripagora.review.repository.ReviewRepository;
import com.tripagora.room.entity.Room;
import com.tripagora.room.repository.RoomRepository;
import com.tripagora.trip.dto.request.TripCreateRequest;
import com.tripagora.trip.dto.request.TripSearchRequest;
import com.tripagora.trip.dto.request.TripUpdateRequest;
import com.tripagora.trip.dto.response.*;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripItinerary;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.exception.*;
import com.tripagora.trip.repository.TripRepository;
import com.tripagora.template.dto.request.ItineraryItemRequest;
import com.tripagora.template.dto.request.ItineraryUpdateRequest;
import com.tripagora.template.dto.response.ItinerariesResponse;
import com.tripagora.template.dto.response.ItineraryItemResponse;
import com.tripagora.template.entity.Template;
import com.tripagora.template.exception.ItineraryDaySequenceInvalidException;
import com.tripagora.template.service.TemplateService;
import com.tripagora.user.entity.Role;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.repository.UserRepository;
import com.tripagora.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {
    // 가이드 일정 충돌 검사 대상: 아직 끝나지 않은 여행
    private static final List<TripStatus> SCHEDULE_CONFLICT_STATUSES = List.of(
            TripStatus.RECRUITING,
            TripStatus.RECRUITMENT_CLOSED,
            TripStatus.IN_PROGRESS
    );

    private final TemplateService templateService;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final RoomRepository roomRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public TripCreateResponse createTrip(long userId, TripCreateRequest request) {
        User guide = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Template template = templateService.findTemplateAndVerifyOwner(userId, request.templateId());

        LocalDate endDate = Trip.calculateEndDate(template, request.startDate());

        boolean hasConflict = tripRepository.existsConflictingTrip(
                template.getGuideProfile(),
                SCHEDULE_CONFLICT_STATUSES,
                request.startDate(),
                endDate
        );

        if (hasConflict) {
            throw new TripDateConflictException();
        }

        Trip trip = Trip.builder()
                .template(template)
                .maxParticipants(request.maxParticipants())
                .startDate(request.startDate())
                .build();

        List<TripItinerary> tripItineraries = template.getTemplateItineraries().stream()
                .map(templateItinerary -> TripItinerary.builder()
                        .trip(trip)
                        .day(templateItinerary.getDay())
                        .location(templateItinerary.getLocation())
                        .content(templateItinerary.getContent())
                        .startTime(templateItinerary.getStartTime())
                        .latitude(templateItinerary.getLatitude())
                        .longitude(templateItinerary.getLongitude())
                        .build())
                .toList();

        tripItineraries.forEach(trip::addItinerary);

        Trip savedTrip = tripRepository.save(trip);

        Participation guideParticipation = Participation.builder()
                .user(guide)
                .trip(trip)
                .role(Role.GUIDE)
                .build();

        trip.addParticipation(guideParticipation);

        return new TripCreateResponse(savedTrip.getId());
    }

    @Transactional(readOnly = true)
    public TripDetailResponse getTripDetails(long userId, long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        Template template = trip.getTemplate();
        List<Long> regions = template.getTemplateRegions().stream()
                .map(templateRegion -> templateRegion.getRegion().getId())
                .collect(Collectors.toList());

        List<Long> tags = template.getTemplateTags().stream()
                .map(templateTag -> templateTag.getTag().getId())
                .collect(Collectors.toList());

        List<String> images = template.getTemplateImages().stream()
                .map(image -> image.getImageUrl())
                .collect(Collectors.toList());

        List<TripParticipantResponse> participants = trip.getParticipants().stream()
                .map(participation -> {
                    User user = participation.getUser();

                    return new TripParticipantResponse(
                            user.getId(),
                            user.getNickname(),
                            user.getImageUrl(),
                            participation.getRole().name()
                    );
                })
                .toList();
        GuideProfile guideProfile = template.getGuideProfile();
        Long guideProfileId = guideProfile.getId();
        User guide = guideProfile.getUser();
        Long roomId = (trip.getRoom() == null) ? null : trip.getRoom().getId();

        boolean isParticipating = participationRepository.existsByUser_IdAndTrip_Id(userId, tripId);
        boolean isMyTrip = Objects.equals(guide.getId(), userId);
        boolean isInWishlist = wishlistRepository.existsByUser_IdAndTrip_Id(userId, tripId);
        Long reviewId = reviewRepository.findByAuthor_IdAndTrip_Id(userId, tripId)
                .map(Review::getId)
                .orElse(null);
        boolean hasWrittenReview = reviewId != null;

        return new TripDetailResponse(
                template.getId(),
                template.getTitle(),
                template.getContent(),
                regions,
                tags,
                images,
                trip.getMaxParticipants(),
                trip.getCurrentParticipants(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus().name(),
                roomId,
                participants,
                guideProfileId,
                isParticipating,
                isMyTrip,
                isInWishlist,
                hasWrittenReview,
                reviewId
        );
    }

    @Transactional
    public void updateTrip(long userId, long tripId, TripUpdateRequest request) {
        Trip trip = findTripAndVerifyOwner(userId, tripId);

        if (trip.getStatus() != TripStatus.RECRUITING) {
            throw new TripNotRecruitingException();
        }

        if (request.maxParticipants() < trip.getCurrentParticipants()) {
            throw new InvalidMaxParticipantsException();
        }

        if (!request.startDate().equals(trip.getStartDate())) {
            LocalDate newEndDate = Trip.calculateEndDate(trip.getTemplate(), request.startDate());

            boolean hasConflict = tripRepository.existsConflictingTrip(
                    trip.getTemplate().getGuideProfile(),
                    tripId,
                    SCHEDULE_CONFLICT_STATUSES,
                    request.startDate(),
                    newEndDate
            );

            if (hasConflict) {
                throw new TripDateConflictException();
            }
        }

        trip.updateInfo(request.maxParticipants(), request.startDate());
    }

    @Transactional
    public void deleteTrip(long userId, long tripId) {
        Trip trip = findTripAndVerifyOwner(userId, tripId);

        if (trip.getStatus() != TripStatus.RECRUITING) {
            throw new TripDeleteNotAllowedException();
        }

        tripRepository.delete(trip);
    }

    @Transactional(readOnly = true)
    public TripListResponse getMyTrips(long userId, List<TripStatus> statuses, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        GuideProfile guideProfile = user.getGuideProfile();
        if (guideProfile == null) {
            throw new GuideProfileNotFoundException();
        }

        Slice<Trip> tripSlice;
        if (statuses == null || statuses.isEmpty()) {
            tripSlice = tripRepository.findByTemplate_GuideProfile(guideProfile, pageable);
        } else {
            tripSlice = tripRepository.findByTemplate_GuideProfileAndStatusIn(guideProfile, statuses, pageable);
        }

        return getTripListResponse(tripSlice);
    }

    @Transactional(readOnly = true)
    public TripListResponse getParticipatingTrips(long userId, List<TripStatus> statuses, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Slice<Participation> participationSlice;
        if (statuses == null || statuses.isEmpty()) {
            participationSlice = participationRepository.findByUserAndRole(user, Role.TRAVELER, pageable);
        } else {
            participationSlice = participationRepository.findByUserAndRoleAndTrip_StatusIn(user, Role.TRAVELER, statuses, pageable);
        }

        Slice<Trip> tripSlice = participationSlice.map(Participation::getTrip);

        return getTripListResponse(tripSlice);
    }

    @Transactional(readOnly = true)
    public CompletedTripListResponse getCompletedTrips(long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Slice<Participation> participationSlice = participationRepository.findByUserAndRoleAndTrip_Status(
                user,
                Role.TRAVELER,
                TripStatus.COMPLETED,
                pageable
        );

        List<CompletedTripResponse> completedTrips = participationSlice.getContent().stream()
                .map(participation -> {
                    Trip trip = participation.getTrip();
                    Template template = trip.getTemplate();

                    String imageUrl = template.getTemplateImages().isEmpty() ?
                            null : template.getTemplateImages().get(0).getImageUrl();

                    List<Long> regions = template.getTemplateRegions().stream()
                            .map(tr -> tr.getRegion().getId())
                            .toList();

                    Long roomId = (trip.getRoom() == null) ? null : trip.getRoom().getId();
                    Long reviewId = reviewRepository.findByAuthor_IdAndTrip_Id(userId, trip.getId())
                            .map(Review::getId)
                            .orElse(null);
                    boolean hasWrittenReview = reviewId != null;

                    return new CompletedTripResponse(
                            trip.getId(),
                            template.getTitle(),
                            imageUrl,
                            regions,
                            trip.getMaxParticipants(),
                            trip.getCurrentParticipants(),
                            trip.getStartDate(),
                            trip.getEndDate(),
                            trip.getStatus().name(),
                            roomId,
                            hasWrittenReview,
                            reviewId
                    );
                })
                .toList();

        return new CompletedTripListResponse(completedTrips, participationSlice.hasNext());
    }

    @Transactional(readOnly = true)
    public ItinerariesResponse getItineraries(long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        List<ItineraryItemResponse> itineraries = trip.getTripItineraries().stream()
                .map(itinerary -> new ItineraryItemResponse(
                        itinerary.getId(),
                        itinerary.getDay(),
                        itinerary.getLocation(),
                        itinerary.getContent(),
                        itinerary.getStartTime(),
                        itinerary.getLatitude(),
                        itinerary.getLongitude()))
                .toList();

        return new ItinerariesResponse(itineraries);
    }

    @Transactional
    public void closeRecruitment(long userId, long tripId) {
        Trip trip = findTripAndVerifyOwner(userId, tripId);

        LocalDate today = LocalDate.now();
        if (trip.getEndDate().isBefore(today)) {
            throw new TripAlreadyEndedException();
        }

        if (trip.getStatus() != TripStatus.RECRUITING) {
            throw new TripNotRecruitingException();
        }

        TripStatus newStatus;
        if (trip.getStartDate().isAfter(today)) {
            newStatus = TripStatus.RECRUITMENT_CLOSED;
        } else {
            newStatus = TripStatus.IN_PROGRESS;
        }
        trip.updateStatus(newStatus);

        Room room = Room.builder()
                .trip(trip)
                .build();
        roomRepository.save(room);
        trip.assignRoom(room);
    }

    @Transactional(readOnly = true)
    public TripListResponse searchTrips(TripSearchRequest request, Pageable pageable) {
        Slice<Trip> tripSlice = tripRepository.search(request, pageable);

        return getTripListResponse(tripSlice);
    }

    @Transactional
    public void updateItineraries(Long userId, Long tripId, ItineraryUpdateRequest request) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);
        Participation participation = participationRepository.findByUser_IdAndTrip_Id(userId, tripId)
                .orElseThrow(ParticipationNotFoundException::new);

        if(participation.getRole() != Role.GUIDE) {
            throw new AccessDeniedException();
        }

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new TripNotInProgressException();
        }

        Map<Integer, List<ItineraryItemRequest>> itinerariesByDay = request.itineraries().stream()
                .collect(Collectors.groupingBy(ItineraryItemRequest::day));

        int maxDay = itinerariesByDay.keySet().stream().max(Integer::compareTo).orElse(0);
        for (int i = 1; i <= maxDay; i++) {
            if (!itinerariesByDay.containsKey(i)) {
                throw new ItineraryDaySequenceInvalidException();
            }
        }

        trip.clearItineraries();
        request.itineraries().forEach(item -> trip.addItinerary(item.toTripItinerary(trip)));
    }

    private Trip findTripAndVerifyOwner(long userId, long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        if (!trip.getTemplate().getGuideProfile().getUser().getId().equals(userId)) {
            throw new AccessDeniedException();
        }

        return trip;
    }

    public static TripListResponse getTripListResponse(Slice<Trip> tripSlice) {
        List<TripSummaryResponse> summaries = tripSlice.getContent().stream()
                .map(trip -> {
                    Template template = trip.getTemplate();
                    String imageUrl = template.getTemplateImages().isEmpty() ? null : template.getTemplateImages().get(0).getImageUrl();

                    List<Long> regions = template.getTemplateRegions().stream()
                            .map(tr -> tr.getRegion().getId())
                            .toList();

                    Long roomId = (trip.getRoom() == null) ? null : trip.getRoom().getId();

                    return new TripSummaryResponse(
                            trip.getId(),
                            template.getTitle(),
                            imageUrl,
                            regions,
                            trip.getMaxParticipants(),
                            trip.getCurrentParticipants(),
                            trip.getStartDate(),
                            trip.getEndDate(),
                            trip.getStatus().name(),
                            roomId
                    );
                })
                .toList();

        return new TripListResponse(summaries, tripSlice.hasNext());
    }
}