package com.tripagora.trip.repository;

import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long>, TripRepositoryCustom {
    // 여행 행(인원 수·정원·상태)을 바꾸는 작업을 줄 세운다: 참가 신청·취소, 정원 수정, 모집 마감, 여행 삭제
    // 비관적 락(SELECT ... FOR UPDATE). 트랜잭션의 첫 읽기로 잡아야 한다: 먼저 다른 것을 읽으면 그때 잡힌
    // REPEATABLE READ 스냅샷 때문에 락을 얻은 뒤의 조회가 앞 트랜잭션이 커밋한 결과를 보지 못한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :tripId")
    Optional<Trip> findByIdForUpdate(@Param("tripId") Long tripId);

    // 특정 가이드의 여행 전체를 페이징 조회
    Slice<Trip> findByTemplate_GuideProfile(GuideProfile guideProfile, Pageable pageable);

    // 특정 가이드의 여행을 상태별로 페이징 조회
    Slice<Trip> findByTemplate_GuideProfileAndStatusIn(
            GuideProfile guideProfile, List<TripStatus> statuses, Pageable pageable);

    // 특정 가이드의 특정 날짜 범위와 겹치는 여행이 있는지 확인 (특정 여행 제외)
    @Query("SELECT COUNT(s) > 0 FROM Trip s " +
            "WHERE s.template.guideProfile = :guideProfile " +
            "AND s.id != :excludeTripId " +
            "AND s.status IN :statuses " +
            "AND s.startDate <= :endDate " +
            "AND s.endDate >= :startDate")
    boolean existsConflictingTrip(
            @Param("guideProfile") GuideProfile guideProfile,
            @Param("excludeTripId") Long excludeTripId,
            @Param("statuses") List<TripStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 특정 가이드의 특정 날짜 범위와 겹치는 여행이 있는지 확인 (신규 생성용)
    @Query("SELECT COUNT(s) > 0 FROM Trip s " +
            "WHERE s.template.guideProfile = :guideProfile " +
            "AND s.status IN :statuses " +
            "AND s.startDate <= :endDate " +
            "AND s.endDate >= :startDate")
    boolean existsConflictingTrip(
            @Param("guideProfile") GuideProfile guideProfile,
            @Param("statuses") List<TripStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 특정 템플릿에 활성 여행이 있는지 확인
    @Query("SELECT COUNT(s) > 0 FROM Trip s " +
            "WHERE s.template.id = :templateId " +
            "AND s.status IN ('RECRUITING', 'RECRUITMENT_CLOSED', 'IN_PROGRESS')")
    boolean existsActiveTripByTemplateId(@Param("templateId") Long templateId);

    boolean existsByTemplate_Id(Long templateId);

    // 시작일이 됐거나 지난 여행 (스케줄러가 못 돈 날이 있어도 다음 실행에서 따라잡는다)
    List<Trip> findByStatusAndStartDateLessThanEqual(TripStatus status, LocalDate today);
    List<Trip> findByStatusAndEndDateBefore(TripStatus status, LocalDate yesterday);
}