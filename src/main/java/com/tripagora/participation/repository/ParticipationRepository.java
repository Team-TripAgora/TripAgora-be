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
    Optional<Participation> findByUser_IdAndTrip_Id(long userId, long tripId);

    // 채팅방 참가 확인: 채팅방 id와 사용자 id로 그 여행의 참가를 한 번에 읽는다 (사용자·여행 함께. 채팅방 id는 여행의 외래 키)
    // 사용자의 가이드 프로필은 역방향 1:1이라 함께 가져오지 않으면 메시지마다 따로 조회된다
    @Query("SELECT p FROM Participation p " +
            "JOIN FETCH p.user u " +
            "LEFT JOIN FETCH u.guideProfile " +
            "JOIN FETCH p.trip t " +
            "WHERE t.room.id = :roomId AND u.id = :userId")
    Optional<Participation> findByRoomIdAndUserId(@Param("roomId") long roomId, @Param("userId") long userId);

    // SOS 받는 사람: 그 여행 참가자 중 보낸 사람을 빼고, FCM 토큰이 있는 사람의 토큰
    @Query("SELECT u.fcmToken FROM Participation p JOIN p.user u " +
            "WHERE p.trip.id = :tripId AND u.id <> :senderId " +
            "AND u.fcmToken IS NOT NULL AND TRIM(u.fcmToken) <> ''")
    List<String> findFcmTokensOfOtherParticipants(@Param("tripId") long tripId, @Param("senderId") long senderId);
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