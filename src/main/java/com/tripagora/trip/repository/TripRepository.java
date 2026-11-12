package com.tripagora.trip.repository;

import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long>, TripRepositoryCustom {
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

    List<Trip> findByStatusAndStartDate(TripStatus status, LocalDate startDate);
    List<Trip> findByStatusAndEndDateBefore(TripStatus status, LocalDate yesterday);
}