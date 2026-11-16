package com.tripagora.participation.service;

import com.tripagora.common.exception.UniqueConstraints;
import com.tripagora.participation.dto.response.ParticipationResponse;
import com.tripagora.participation.entity.Participation;
import com.tripagora.participation.exception.*;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.exception.TripNotFoundException;
import com.tripagora.trip.repository.TripRepository;
import com.tripagora.user.entity.Role;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParticipationService {
    private final ParticipationRepository participationRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public ParticipationResponse applyForTrip(Long userId, Long tripId) {
        // 사용자 → 여행 순서로 잠근다 (순서를 고정해 데드락을 막는다). 같은 사용자의 신청과
        // 같은 여행의 인원 변경이 한 번에 하나씩 처리되므로, 아래 정원·중복·일정 확인이 최신 상태로 이루어진다
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);
        Trip trip = tripRepository.findByIdForUpdate(tripId).orElseThrow(TripNotFoundException::new);

        trip.validateJoinable(LocalDate.now(clock));

        if (trip.getTemplate().getGuideProfile().getUser().getId().equals(userId)) {
            throw new CannotParticipateInOwnTripException();
        }

        participationRepository.findByUser_IdAndTrip_Id(userId, tripId).ifPresent(p -> {
            throw new AlreadyParticipatingException();
        });

        boolean hasConflict = participationRepository.existsConflictingParticipation(
                userId,
                trip.getStartDate(),
                trip.getEndDate(),
                TripStatus.COMPLETED
        );

        if (hasConflict) {
            throw new ConflictingParticipationException();
        }

        trip.increaseParticipant();

        Participation participation = Participation.builder()
                .user(user)
                .trip(trip)
                .role(Role.TRAVELER)
                .build();

        try {
            participationRepository.save(participation);
        } catch (DataIntegrityViolationException e) {
            // 같은 사용자의 같은 여행 신청이 동시에 들어와 먼저 저장된 것이 있으면
            if (UniqueConstraints.isViolated(e, Participation.UNIQUE_USER_TRIP)) {
                throw new AlreadyParticipatingException();
            }
            throw e;
        }
        trip.addParticipation(participation);

        return new ParticipationResponse(
                participation.getId(),
                trip.getId(),
                trip.getCurrentParticipants()
        );
    }

    @Transactional
    public void cancelParticipation(Long userId, Long tripId) {
        Trip trip = tripRepository.findByIdForUpdate(tripId).orElseThrow(TripNotFoundException::new);

        Participation participation = participationRepository.findByUser_IdAndTrip_Id(userId, tripId)
                .orElseThrow(ParticipationNotFoundException::new);

        participation.validateCancellable();

        // 완료된 여행은 참가 기록을 남기고 내 목록에서만 숨긴다 (다른 참가자가 보는 이력과 인원은 그대로)
        if (trip.isCompleted()) {
            participation.hide(LocalDateTime.now(clock));
            return;
        }

        trip.validateCancellable();
        trip.decreaseParticipant();
        participationRepository.delete(participation);
    }
}