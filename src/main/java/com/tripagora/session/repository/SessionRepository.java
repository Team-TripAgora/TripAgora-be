package com.tripagora.session.repository;

import com.tripagora.guideProfile.entity.GuideProfile;
import com.tripagora.session.entity.Session;
import com.tripagora.session.entity.SessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long>, SessionRepositoryCustom {
    // 특정 가이드의 세션 전체를 페이징 조회
    Slice<Session> findByTemplate_GuideProfile(GuideProfile guideProfile, Pageable pageable);

    // 특정 가이드의 세션을 상태별로 페이징 조회
    Slice<Session> findByTemplate_GuideProfileAndStatusIn(
            GuideProfile guideProfile, List<SessionStatus> statuses, Pageable pageable);

    // 특정 가이드의 특정 날짜 범위와 겹치는 세션이 있는지 확인 (특정 세션 제외)
    @Query("SELECT COUNT(s) > 0 FROM Session s " +
            "WHERE s.template.guideProfile = :guideProfile " +
            "AND s.id != :excludeSessionId " +
            "AND s.status IN :statuses " +
            "AND s.startDate <= :endDate " +
            "AND s.endDate >= :startDate")
    boolean existsConflictingSession(
            @Param("guideProfile") GuideProfile guideProfile,
            @Param("excludeSessionId") Long excludeSessionId,
            @Param("statuses") List<SessionStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 특정 가이드의 특정 날짜 범위와 겹치는 세션이 있는지 확인 (신규 생성용)
    @Query("SELECT COUNT(s) > 0 FROM Session s " +
            "WHERE s.template.guideProfile = :guideProfile " +
            "AND s.status IN :statuses " +
            "AND s.startDate <= :endDate " +
            "AND s.endDate >= :startDate")
    boolean existsConflictingSession(
            @Param("guideProfile") GuideProfile guideProfile,
            @Param("statuses") List<SessionStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 특정 템플릿에 활성 세션이 있는지 확인
    @Query("SELECT COUNT(s) > 0 FROM Session s " +
            "WHERE s.template.id = :templateId " +
            "AND s.status IN ('RECRUITING', 'RECRUITMENT_CLOSED', 'IN_PROGRESS')")
    boolean existsActiveSessionByTemplateId(@Param("templateId") Long templateId);

    List<Session> findByStatusAndStartDate(SessionStatus status, LocalDate startDate);
    List<Session> findByStatusAndEndDateBefore(SessionStatus status, LocalDate yesterday);
}