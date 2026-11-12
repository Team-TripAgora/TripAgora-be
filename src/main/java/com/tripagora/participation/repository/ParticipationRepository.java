package com.tripagora.participation.repository;

import com.tripagora.participation.entity.Participation;
import com.tripagora.session.entity.SessionStatus;
import com.tripagora.user.entity.Role;
import com.tripagora.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    // TODO: 두 메서드 합치기?
    Optional<Participation> findByUser_IdAndSession_Id(long userId, long sessionId);
    boolean existsByUser_IdAndSession_Id(long userId, long sessionId);

    Slice<Participation> findByUserAndRoleAndSession_Status(
            User user,
            Role role,
            SessionStatus sessionStatus,
            Pageable pageable);

    Slice<Participation> findByUserAndRoleAndSession_StatusIn(
            User user,
            Role role,
            List<SessionStatus> statuses,
            Pageable pageable
    );

    Slice<Participation> findByUserAndRole(
            User user,
            Role role,
            Pageable pageable
    );

    @Query("SELECT COUNT(p) > 0 " +
            "FROM Participation p " +
            "WHERE p.user.id = :userId " +
            "AND p.session.status != :completedStatus " +
            "AND p.session.startDate <= :endDate " +
            "AND p.session.endDate >= :startDate")
    boolean existsConflictingParticipation(@Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("completedStatus") SessionStatus completedStatus);
}