package com.example.TripAgora.participation.service;

import com.example.TripAgora.participation.dto.response.ParticipationResponse;
import com.example.TripAgora.participation.entity.Participation;
import com.example.TripAgora.participation.exception.*;
import com.example.TripAgora.participation.repository.ParticipationRepository;
import com.example.TripAgora.session.entity.Session;
import com.example.TripAgora.session.entity.SessionStatus;
import com.example.TripAgora.session.exception.SessionNotFoundException;
import com.example.TripAgora.session.repository.SessionRepository;
import com.example.TripAgora.user.entity.Role;
import com.example.TripAgora.user.entity.User;
import com.example.TripAgora.user.exception.UserNotFoundException;
import com.example.TripAgora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipationService {
    private final ParticipationRepository participationRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ParticipationResponse applyForSession(Long userId, Long sessionId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Session session = sessionRepository.findById(sessionId).orElseThrow(SessionNotFoundException::new);

        validateSessionStatus(session);

        if (session.getTemplate().getGuideProfile().getUser().getId().equals(userId)) {
            throw new CannotParticipateInOwnSessionException();
        }

        participationRepository.findByUser_IdAndSession_Id(userId, sessionId).ifPresent(p -> {
            throw new AlreadyParticipatingException();
        });

        boolean hasConflict = participationRepository.existsConflictingParticipation(
                userId,
                session.getStartDate(),
                session.getEndDate(),
                SessionStatus.COMPLETED
        );

        if (hasConflict) {
            throw new ConflictingParticipationException();
        }

        session.increaseParticipant();

        Participation participation = Participation.builder()
                .user(user)
                .session(session)
                .role(Role.TRAVELER)
                .build();

        participationRepository.save(participation);
        session.addParticipation(participation);

        return new ParticipationResponse(
                participation.getId(),
                session.getId(),
                session.getCurrentParticipants()
        );
    }

    @Transactional
    public void cancelParticipation(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow(SessionNotFoundException::new);

        Participation participation = participationRepository.findByUser_IdAndSession_Id(userId, sessionId)
                .orElseThrow(ParticipationNotFoundException::new);

        validateSessionStatus(session);

        session.decreaseParticipant();
        participationRepository.delete(participation);
    }

    private void validateSessionStatus(Session session) {
        if (session.getStatus() != SessionStatus.RECRUITING &&
            session.getStatus() != SessionStatus.COMPLETED) {
            throw new ParticipationCancelNotAllowedException();
        }
    }
}