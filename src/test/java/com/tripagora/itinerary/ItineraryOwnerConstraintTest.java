package com.tripagora.itinerary;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 일정 테이블은 템플릿 일정과 여행 일정을 함께 담는다. 한 행은 템플릿과 여행 중 정확히 한 곳에 속해야 한다 (DB CHECK 제약)
class ItineraryOwnerConstraintTest extends IntegrationTest {
    private static final String INSERT = "INSERT INTO itinerary (created_at, modified_at, content, day, latitude, location, longitude, start_time, template_id, trip_id) "
            + "VALUES (NOW(6), NOW(6), '내용', 1, 37.5, '장소', 127.0, '10:00:00', ?, ?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        fixture.guide(1L);
        fixture.template(1L, 1L, 0);
        fixture.trip(1L, 1L, TripStatus.RECRUITING, today.plusDays(10), today.plusDays(10));
    }

    @Test
    @DisplayName("템플릿이나 여행 한 곳에만 속한 일정은 저장된다")
    void savesItineraryWithExactlyOneOwner() {
        insert(1L, null);
        insert(null, 1L);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM itinerary", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("주인이 없는 일정은 DB가 거부한다")
    void rejectsItineraryWithoutOwner() {
        assertThatThrownBy(() -> insert(null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_itinerary_owner");
    }

    @Test
    @DisplayName("템플릿과 여행 둘 다에 속한 일정은 DB가 거부한다")
    void rejectsItineraryWithTwoOwners() {
        assertThatThrownBy(() -> insert(1L, 1L))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_itinerary_owner");
    }

    private void insert(Long templateId, Long tripId) {
        jdbcTemplate.update(INSERT, new Object[]{templateId, tripId});
    }
}
