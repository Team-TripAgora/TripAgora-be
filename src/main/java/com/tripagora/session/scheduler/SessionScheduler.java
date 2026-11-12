package com.tripagora.session.scheduler;

import com.tripagora.session.entity.Session;
import com.tripagora.session.entity.SessionStatus;
import com.tripagora.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionScheduler {
    private final SessionRepository sessionRepository;

    @Transactional
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    public void startSessions() {
        LocalDate today = LocalDate.now();
        log.info("[SessionScheduler] 'IN_PROGRESS' 상태 변경 작업 시작 (대상 날짜: {})", today);

        List<Session> sessionsToStart = sessionRepository.findByStatusAndStartDate(
                SessionStatus.RECRUITMENT_CLOSED, today
        );

        sessionsToStart.forEach(session -> {
            session.updateStatus(SessionStatus.IN_PROGRESS);
            log.info(" - 세션 ID {} 상태 변경: IN_PROGRESS (시작일 도래)", session.getId());
        });

        log.info("[SessionScheduler] 'IN_PROGRESS' 상태 변경 작업 완료 (총 {}건)", sessionsToStart.size());
    }

    @Transactional
    @Scheduled(cron = "0 3 0 * * *", zone = "Asia/Seoul")
    public void completeSessions() {
        LocalDate today = LocalDate.now();
        log.info("[SessionScheduler] 'COMPLETED' 상태 변경 작업 시작 (대상 날짜: {} 이전)", today);

        List<Session> sessionsToComplete = sessionRepository.findByStatusAndEndDateBefore(
                SessionStatus.IN_PROGRESS,
                today
        );

        sessionsToComplete.forEach(session -> {
            session.updateStatus(SessionStatus.COMPLETED);
            log.info(" - 세션 ID {} 상태 변경: COMPLETED (종료일 경과)", session.getId());
        });

        log.info("[SessionScheduler] 'COMPLETED' 상태 변경 작업 완료 (총 {}건)", sessionsToComplete.size());
    }
}