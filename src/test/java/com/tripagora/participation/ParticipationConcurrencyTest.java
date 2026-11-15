package com.tripagora.participation;

import com.tripagora.support.ConcurrentRequests;
import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// 참가 신청이 동시에 몰릴 때: 정원, 같은 사람의 중복 신청, 같은 사람의 겹치는 일정
class ParticipationConcurrencyTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;
    private static final long TRIP = 1L;
    private static final long TRAVELER = 101L;
    private static final int CAPACITY = 10;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final LocalDate startDate = today().plusDays(30);

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 1);
    }

    @Test
    @DisplayName("정원 10명인 여행에 30명이 동시에 신청해도 10명만 참가하고, 참가 기록 수와 인원 수가 같다")
    void keepsCapacityUnderConcurrentApplications() throws Exception {
        recruitingTrip(TRIP);
        int applicants = 30;
        for (int i = 0; i < applicants; i++) {
            fixture.traveler(TRAVELER + i);
        }

        ConcurrentRequests.Result result = ConcurrentRequests.send(mockMvc, applicants, i -> apply(TRAVELER + i, TRIP));

        assertThat(result.count(200)).isEqualTo(CAPACITY);
        assertThat(result.count(409)).isEqualTo(applicants - CAPACITY);
        assertThat(travelerParticipations("trip_id = " + TRIP)).isEqualTo(CAPACITY);
        assertThat(currentParticipants(TRIP)).isEqualTo(CAPACITY);
    }

    @Test
    @DisplayName("같은 사람이 같은 여행에 동시에 5번 신청해도 참가는 한 번만 된다")
    void acceptsOnlyOneOfDuplicateApplications() throws Exception {
        recruitingTrip(TRIP);
        fixture.traveler(TRAVELER);

        ConcurrentRequests.Result result = ConcurrentRequests.send(mockMvc, 5, i -> apply(TRAVELER, TRIP));

        assertThat(result.count(200)).isEqualTo(1);
        assertThat(result.count(409)).isEqualTo(4);
        assertThat(travelerParticipations("user_id = " + TRAVELER)).isEqualTo(1);
        assertThat(currentParticipants(TRIP)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 사람이 날짜가 겹치는 여행 5곳에 동시에 신청해도 한 곳만 참가된다")
    void acceptsOnlyOneOfOverlappingTrips() throws Exception {
        int trips = 5;
        for (long tripId = 1; tripId <= trips; tripId++) {
            recruitingTrip(tripId);
        }
        fixture.traveler(TRAVELER);

        ConcurrentRequests.Result result = ConcurrentRequests.send(mockMvc, trips, i -> apply(TRAVELER, i + 1));

        assertThat(result.count(200)).isEqualTo(1);
        assertThat(result.count(400)).isEqualTo(trips - 1);
        assertThat(travelerParticipations("user_id = " + TRAVELER)).isEqualTo(1);
    }

    @Test
    @DisplayName("정원을 줄이는 수정과 신청이 동시에 와도 인원 수가 정원을 넘지 않고 참가 기록 수와 같다")
    void keepsCapacityWhenCapacityIsReducedDuringApplications() throws Exception {
        recruitingTrip(TRIP);
        joined(TRIP, 9);
        int applicants = 5;
        for (int i = 0; i < applicants; i++) {
            fixture.traveler(TRAVELER + 100 + i);
        }

        // 0번은 가이드가 정원을 9로 줄이는 요청, 나머지는 새 신청
        ConcurrentRequests.Result result = ConcurrentRequests.send(mockMvc, applicants + 1, i -> i == 0
                ? patch("/api/trips/" + TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":9,\"startDate\":\"" + startDate + "\"}")
                : apply(TRAVELER + 100 + i - 1, TRIP));

        assertThat(result.count(500)).isZero();
        int current = currentParticipants(TRIP);
        assertThat(current).isEqualTo(travelerParticipations("trip_id = " + TRIP));
        assertThat(current).isLessThanOrEqualTo(maxParticipants(TRIP));
    }

    @Test
    @DisplayName("꽉 찬 여행에서 참가 취소와 신청이 동시에 와도 참가 기록 수와 인원 수가 같다")
    void keepsCountWhenCancelAndApplicationsRace() throws Exception {
        recruitingTrip(TRIP);
        joined(TRIP, CAPACITY);
        int applicants = 5;
        for (int i = 0; i < applicants; i++) {
            fixture.traveler(TRAVELER + 100 + i);
        }

        // 0번은 이미 참가한 사람의 취소, 나머지는 새 신청 (빈자리는 많아야 1개)
        ConcurrentRequests.Result result = ConcurrentRequests.send(mockMvc, applicants + 1, i -> i == 0
                ? delete("/api/participation/trips/" + TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER))
                : apply(TRAVELER + 100 + i - 1, TRIP));

        assertThat(result.count(500)).isZero();
        int current = currentParticipants(TRIP);
        assertThat(current).isEqualTo(travelerParticipations("trip_id = " + TRIP));
        assertThat(current).isLessThanOrEqualTo(CAPACITY);
    }

    @Test
    @DisplayName("모집중 여행 삭제와 신청이 동시에 와도 500 없이 여행과 참가 기록이 함께 지워진다")
    void deletesTripSafelyDuringApplications() throws Exception {
        recruitingTrip(TRIP);
        int applicants = 5;
        for (int i = 0; i < applicants; i++) {
            fixture.traveler(TRAVELER + 100 + i);
        }

        // 0번은 가이드의 여행 삭제, 나머지는 새 신청 (삭제가 먼저면 신청은 404, 신청이 먼저면 함께 지워진다)
        ConcurrentRequests.Result result = ConcurrentRequests.send(mockMvc, applicants + 1, i -> i == 0
                ? delete("/api/trips/" + TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE))
                : apply(TRAVELER + 100 + i - 1, TRIP));

        assertThat(result.count(500)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trip WHERE id = ?", Integer.class, TRIP)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM participation WHERE trip_id = ?", Integer.class, TRIP)).isZero();
    }

    // 여행객 count명이 이미 참가한 상태 (사용자 id 101부터)
    private void joined(long tripId, int count) {
        for (int i = 0; i < count; i++) {
            fixture.traveler(TRAVELER + i);
            fixture.participation(TRAVELER + i, tripId, TRAVELER + i, Role.TRAVELER);
        }
        jdbcTemplate.update("UPDATE trip SET current_participants = ? WHERE id = ?", count, tripId);
    }

    private int maxParticipants(long tripId) {
        return jdbcTemplate.queryForObject("SELECT max_participants FROM trip WHERE id = ?", Integer.class, tripId);
    }

    // 정원 10명, 하루짜리 모집중 여행 (같은 날짜)
    private void recruitingTrip(long tripId) {
        fixture.trip(tripId, TEMPLATE, TripStatus.RECRUITING, startDate, startDate);
        jdbcTemplate.update("UPDATE trip SET max_participants = ? WHERE id = ?", CAPACITY, tripId);
    }

    private RequestBuilder apply(long userId, long tripId) {
        return post("/api/participation/trips/" + tripId).header("Authorization", bearer(userId, Role.TRAVELER));
    }

    private int travelerParticipations(String condition) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM participation WHERE role = 'TRAVELER' AND " + condition, Integer.class);
    }

    private int currentParticipants(long tripId) {
        return jdbcTemplate.queryForObject("SELECT current_participants FROM trip WHERE id = ?", Integer.class, tripId);
    }
}
