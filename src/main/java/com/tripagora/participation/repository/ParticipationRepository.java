package com.tripagora.participation.repository;

import com.tripagora.participation.entity.Participation;
import com.tripagora.trip.entity.TripStatus;
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
    Optional<Participation> findByUser_IdAndTrip_Id(long userId, long tripId);
    boolean existsByUser_IdAndTrip_Id(long userId, long tripId);
    boolean existsByUser_IdAndTrip_StatusIn(long userId, List<TripStatus> statuses);

    // 내 여행 목록 3개: 숨긴 참가(완료 여행을 목록에서 지운 것)는 뺀다
    Slice<Participation> findByUserAndRoleAndHiddenAtIsNullAndTrip_Status(
            User user,
            Role role,
            TripStatus tripStatus,
            Pageable pageable);

    Slice<Participation> findByUserAndRoleAndHiddenAtIsNullAndTrip_StatusIn(
            User user,
            Role role,
            List<TripStatus> statuses,
            Pageable pageable
    );

    Slice<Participation> findByUserAndRoleAndHiddenAtIsNull(
            User user,
            Role role,
            Pageable pageable
    );

    @Query("SELECT COUNT(p) > 0 " +
            "FROM Participation p " +
            "WHERE p.user.id = :userId " +
            "AND p.trip.status != :completedStatus " +
            "AND p.trip.startDate <= :endDate " +
            "AND p.trip.endDate >= :startDate")
    boolean existsConflictingParticipation(@Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("completedStatus") TripStatus completedStatus);
}