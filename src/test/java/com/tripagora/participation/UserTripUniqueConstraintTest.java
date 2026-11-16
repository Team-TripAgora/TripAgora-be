package com.tripagora.participation;

import com.tripagora.support.ConcurrentRequests;
import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// 한 사용자는 한 여행에 한 번만 참가하고 한 번만 찜한다 (DB unique 제약이 마지막에 막는다)
class UserTripUniqueConstraintTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TRIP = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDate startDate = today().plusDays(30);
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(1L, GUIDE, 1);
        fixture.trip(TRIP, 1L, TripStatus.RECRUITING, startDate, startDate);
    }

    @Test
    @DisplayName("같은 사용자·여행의 참가 기록은 DB가 두 번 저장하지 않는다")
    void rejectsDuplicateParticipationRow() {
        fixture.participation(1L, TRIP, TRAVELER, Role.TRAVELER);

        assertThatThrownBy(() -> fixture.participation(2L, TRIP, TRAVELER, Role.TRAVELER))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uk_participation_user_trip");
    }

    @Test
    @DisplayName("같은 여행을 동시에 5번 찜해도 한 번만 저장되고 나머지는 409")
    void savesWishlistOnceUnderConcurrentRequests() throws Exception {
        ConcurrentRequests.Result result = ConcurrentRequests.send(mockMvc, 5,
                i -> post("/api/wishlist/" + TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)));

        assertThat(result.count(201)).isEqualTo(1);
        assertThat(result.count(409)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wishlist WHERE user_id = ? AND trip_id = ?",
                Integer.class, TRAVELER, TRIP)).isEqualTo(1);
    }
}
