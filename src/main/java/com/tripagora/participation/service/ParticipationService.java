package com.tripagora.participation.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipationService {
    private final ParticipationRepository participationRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional
    public ParticipationResponse applyForTrip(Long userId, Long tripId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        trip.validateJoinable();

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

        participationRepository.save(participation);
        trip.addParticipation(participation);

        return new ParticipationResponse(
                participation.getId(),
                trip.getId(),
                trip.getCurrentParticipants()
        );
    }

    @Transactional
    public void cancelParticipation(Long userId, Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        Participation participation = participationRepository.findByUser_IdAndTrip_Id(userId, tripId)
                .orElseThrow(ParticipationNotFoundException::new);

        participation.validateCancellable();

        // 완료된 여행은 참가 기록을 남기고 내 목록에서만 숨긴다 (다른 참가자가 보는 이력과 인원은 그대로)
        if (trip.isCompleted()) {
            participation.hide();
            return;
        }

        trip.validateCancellable();
        trip.decreaseParticipant();
        participationRepository.delete(participation);
    }
}