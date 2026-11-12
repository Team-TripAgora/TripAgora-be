package com.tripagora.session.service;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.guideProfile.entity.GuideProfile;
import com.tripagora.guideProfile.exception.GuideProfileNotFoundException;
import com.tripagora.participation.entity.Participation;
import com.tripagora.participation.exception.ParticipationNotFoundException;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.review.entity.Review;
import com.tripagora.review.repository.ReviewRepository;
import com.tripagora.room.entity.Room;
import com.tripagora.room.repository.RoomRepository;
import com.tripagora.session.dto.request.SessionCreateRequest;
import com.tripagora.session.dto.request.SessionSearchRequest;
import com.tripagora.session.dto.request.SessionUpdateRequest;
import com.tripagora.session.dto.response.*;
import com.tripagora.session.entity.Session;
import com.tripagora.session.entity.SessionItinerary;
import com.tripagora.session.entity.SessionStatus;
import com.tripagora.session.exception.*;
import com.tripagora.session.repository.SessionRepository;
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
public class SessionService {
    // 가이드 일정 충돌 검사 대상: 아직 끝나지 않은 여행
    private static final List<SessionStatus> SCHEDULE_CONFLICT_STATUSES = List.of(
            SessionStatus.RECRUITING,
            SessionStatus.RECRUITMENT_CLOSED,
            SessionStatus.IN_PROGRESS
    );

    private final TemplateService templateService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final RoomRepository roomRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public SessionCreateResponse createSession(long userId, SessionCreateRequest request) {
        User guide = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Template template = templateService.findTemplateAndVerifyOwner(userId, request.templateId());

        LocalDate endDate = Session.calculateEndDate(template, request.startDate());

        boolean hasConflict = sessionRepository.existsConflictingSession(
                template.getGuideProfile(),
                SCHEDULE_CONFLICT_STATUSES,
                request.startDate(),
                endDate
        );

        if (hasConflict) {
            throw new SessionDateConflictException();
        }

        Session session = Session.builder()
                .template(template)
                .maxParticipants(request.maxParticipants())
                .startDate(request.startDate())
                .build();

        List<SessionItinerary> sessionItineraries = template.getTemplateItineraries().stream()
                .map(templateItinerary -> SessionItinerary.builder()
                        .session(session)
                        .day(templateItinerary.getDay())
                        .location(templateItinerary.getLocation())
                        .content(templateItinerary.getContent())
                        .startTime(templateItinerary.getStartTime())
                        .latitude(templateItinerary.getLatitude())
                        .longitude(templateItinerary.getLongitude())
                        .build())
                .toList();

        sessionItineraries.forEach(session::addItinerary);

        Session savedSession = sessionRepository.save(session);

        Participation guideParticipation = Participation.builder()
                .user(guide)
                .session(session)
                .role(Role.GUIDE)
                .build();

        session.addParticipation(guideParticipation);

        return new SessionCreateResponse(savedSession.getId());
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse getSessionDetails(long userId, long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow(SessionNotFoundException::new);

        Template template = session.getTemplate();
        List<Long> regions = template.getTemplateRegions().stream()
                .map(templateRegion -> templateRegion.getRegion().getId())
                .collect(Collectors.toList());

        List<Long> tags = template.getTemplateTags().stream()
                .map(templateTag -> templateTag.getTag().getId())
                .collect(Collectors.toList());

        List<String> images = template.getTemplateImages().stream()
                .map(image -> image.getImageUrl())
                .collect(Collectors.toList());

        List<SessionParticipantResponse> participants = session.getParticipants().stream()
                .map(participation -> {
                    User user = participation.getUser();

                    return new SessionParticipantResponse(
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
        Long roomId = (session.getRoom() == null) ? null : session.getRoom().getId();

        boolean isParticipating = participationRepository.existsByUser_IdAndSession_Id(userId, sessionId);
        boolean isMySession = Objects.equals(guide.getId(), userId);
        boolean isInWishlist = wishlistRepository.existsByUser_IdAndSession_Id(userId, sessionId);
        Long reviewId = reviewRepository.findByAuthor_IdAndSession_Id(userId, sessionId)
                .map(Review::getId)
                .orElse(null);
        boolean hasWrittenReview = reviewId != null;

        return new SessionDetailResponse(
                template.getId(),
                template.getTitle(),
                template.getContent(),
                regions,
                tags,
                images,
                session.getMaxParticipants(),
                session.getCurrentParticipants(),
                session.getStartDate(),
                session.getEndDate(),
                session.getStatus().name(),
                roomId,
                participants,
                guideProfileId,
                isParticipating,
                isMySession,
                isInWishlist,
                hasWrittenReview,
                reviewId
        );
    }

    @Transactional
    public void updateSession(long userId, long sessionId, SessionUpdateRequest request) {
        Session session = findSessionAndVerifyOwner(userId, sessionId);

        if (session.getStatus() != SessionStatus.RECRUITING) {
            throw new SessionNotRecruitingException();
        }

        if (request.maxParticipants() < session.getCurrentParticipants()) {
            throw new InvalidMaxParticipantsException();
        }

        if (!request.startDate().equals(session.getStartDate())) {
            LocalDate newEndDate = Session.calculateEndDate(session.getTemplate(), request.startDate());

            boolean hasConflict = sessionRepository.existsConflictingSession(
                    session.getTemplate().getGuideProfile(),
                    sessionId,
                    SCHEDULE_CONFLICT_STATUSES,
                    request.startDate(),
                    newEndDate
            );

            if (hasConflict) {
                throw new SessionDateConflictException();
            }
        }

        session.updateInfo(request.maxParticipants(), request.startDate());
    }

    @Transactional
    public void deleteSession(long userId, long sessionId) {
        Session session = findSessionAndVerifyOwner(userId, sessionId);

        if (session.getStatus() != SessionStatus.RECRUITING) {
            throw new SessionDeleteNotAllowedException();
        }

        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public SessionListResponse getMySessions(long userId, List<SessionStatus> statuses, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        GuideProfile guideProfile = user.getGuideProfile();
        if (guideProfile == null) {
            throw new GuideProfileNotFoundException();
        }

        Slice<Session> sessionSlice;
        if (statuses == null || statuses.isEmpty()) {
            sessionSlice = sessionRepository.findByTemplate_GuideProfile(guideProfile, pageable);
        } else {
            sessionSlice = sessionRepository.findByTemplate_GuideProfileAndStatusIn(guideProfile, statuses, pageable);
        }

        return getSessionListResponse(sessionSlice);
    }

    @Transactional(readOnly = true)
    public SessionListResponse getParticipatingSessions(long userId, List<SessionStatus> statuses, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Slice<Participation> participationSlice;
        if (statuses == null || statuses.isEmpty()) {
            participationSlice = participationRepository.findByUserAndRole(user, Role.TRAVELER, pageable);
        } else {
            participationSlice = participationRepository.findByUserAndRoleAndSession_StatusIn(user, Role.TRAVELER, statuses, pageable);
        }

        Slice<Session> sessionSlice = participationSlice.map(Participation::getSession);

        return getSessionListResponse(sessionSlice);
    }

    @Transactional(readOnly = true)
    public CompletedSessionListResponse getCompletedSessions(long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Slice<Participation> participationSlice = participationRepository.findByUserAndRoleAndSession_Status(
                user,
                Role.TRAVELER,
                SessionStatus.COMPLETED,
                pageable
        );

        List<CompletedSessionResponse> completedSessions = participationSlice.getContent().stream()
                .map(participation -> {
                    Session session = participation.getSession();
                    Template template = session.getTemplate();

                    String imageUrl = template.getTemplateImages().isEmpty() ?
                            null : template.getTemplateImages().get(0).getImageUrl();

                    List<Long> regions = template.getTemplateRegions().stream()
                            .map(tr -> tr.getRegion().getId())
                            .toList();

                    Long roomId = (session.getRoom() == null) ? null : session.getRoom().getId();
                    Long reviewId = reviewRepository.findByAuthor_IdAndSession_Id(userId, session.getId())
                            .map(Review::getId)
                            .orElse(null);
                    boolean hasWrittenReview = reviewId != null;

                    return new CompletedSessionResponse(
                            session.getId(),
                            template.getTitle(),
                            imageUrl,
                            regions,
                            session.getMaxParticipants(),
                            session.getCurrentParticipants(),
                            session.getStartDate(),
                            session.getEndDate(),
                            session.getStatus().name(),
                            roomId,
                            hasWrittenReview,
                            reviewId
                    );
                })
                .toList();

        return new CompletedSessionListResponse(completedSessions, participationSlice.hasNext());
    }

    @Transactional(readOnly = true)
    public ItinerariesResponse getItineraries(long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow(SessionNotFoundException::new);

        List<ItineraryItemResponse> itineraries = session.getSessionItineraries().stream()
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
    public void closeRecruitment(long userId, long sessionId) {
        Session session = findSessionAndVerifyOwner(userId, sessionId);

        LocalDate today = LocalDate.now();
        if (session.getEndDate().isBefore(today)) {
            throw new SessionAlreadyEndedException();
        }

        if (session.getStatus() != SessionStatus.RECRUITING) {
            throw new SessionNotRecruitingException();
        }

        SessionStatus newStatus;
        if (session.getStartDate().isAfter(today)) {
            newStatus = SessionStatus.RECRUITMENT_CLOSED;
        } else {
            newStatus = SessionStatus.IN_PROGRESS;
        }
        session.updateStatus(newStatus);

        Room room = Room.builder()
                .session(session)
                .build();
        roomRepository.save(room);
        session.assignRoom(room);
    }

    @Transactional(readOnly = true)
    public SessionListResponse searchSessions(SessionSearchRequest request, Pageable pageable) {
        Slice<Session> sessionSlice = sessionRepository.search(request, pageable);

        return getSessionListResponse(sessionSlice);
    }

    @Transactional
    public void updateItineraries(Long userId, Long sessionId, ItineraryUpdateRequest request) {
        Session session = sessionRepository.findById(sessionId).orElseThrow(SessionNotFoundException::new);
        Participation participation = participationRepository.findByUser_IdAndSession_Id(userId, sessionId)
                .orElseThrow(ParticipationNotFoundException::new);

        if(participation.getRole() != Role.GUIDE) {
            throw new AccessDeniedException();
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new SessionNotInProgressException();
        }

        Map<Integer, List<ItineraryItemRequest>> itinerariesByDay = request.itineraries().stream()
                .collect(Collectors.groupingBy(ItineraryItemRequest::day));

        int maxDay = itinerariesByDay.keySet().stream().max(Integer::compareTo).orElse(0);
        for (int i = 1; i <= maxDay; i++) {
            if (!itinerariesByDay.containsKey(i)) {
                throw new ItineraryDaySequenceInvalidException();
            }
        }

        session.clearItineraries();
        request.itineraries().forEach(session::addItinerary);
    }

    private Session findSessionAndVerifyOwner(long userId, long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow(SessionNotFoundException::new);

        if (!session.getTemplate().getGuideProfile().getUser().getId().equals(userId)) {
            throw new AccessDeniedException();
        }

        return session;
    }

    public static SessionListResponse getSessionListResponse(Slice<Session> sessionSlice) {
        List<SessionSummaryResponse> summaries = sessionSlice.getContent().stream()
                .map(session -> {
                    Template template = session.getTemplate();
                    String imageUrl = template.getTemplateImages().isEmpty() ? null : template.getTemplateImages().get(0).getImageUrl();

                    List<Long> regions = template.getTemplateRegions().stream()
                            .map(tr -> tr.getRegion().getId())
                            .toList();

                    Long roomId = (session.getRoom() == null) ? null : session.getRoom().getId();

                    return new SessionSummaryResponse(
                            session.getId(),
                            template.getTitle(),
                            imageUrl,
                            regions,
                            session.getMaxParticipants(),
                            session.getCurrentParticipants(),
                            session.getStartDate(),
                            session.getEndDate(),
                            session.getStatus().name(),
                            roomId
                    );
                })
                .toList();

        return new SessionListResponse(summaries, sessionSlice.hasNext());
    }
}