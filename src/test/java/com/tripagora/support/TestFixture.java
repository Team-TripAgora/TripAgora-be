package com.tripagora.support;

import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

// 테스트 데이터를 SQL로 바로 넣는다. id는 테스트에서 직접 정한다 (테스트마다 테이블을 비우므로 겹치지 않는다)
public class TestFixture {
    private final JdbcTemplate jdbcTemplate;

    public TestFixture(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void user(long id, String nickname, Role role) {
        jdbcTemplate.update("INSERT INTO users (id, created_at, modified_at, image_url, nickname, role, social_id, social_type) " +
                        "VALUES (?, NOW(6), NOW(6), 'https://example.com/user.png', ?, ?, ?, 'KAKAO')",
                id, nickname, role.name(), "social-" + id);
    }

    // 가이드 사용자와 가이드 프로필을 만든다. 가이드 프로필 id는 사용자 id와 같게 둔다
    public void guide(long userId) {
        user(userId, "가이드" + userId, Role.GUIDE);
        jdbcTemplate.update("INSERT INTO guide_profile (id, created_at, modified_at, bio, total_avg_rating, total_review_count, user_id) " +
                "VALUES (?, NOW(6), NOW(6), '테스트 가이드', 0, 0, ?)", userId, userId);
    }

    public void traveler(long userId) {
        user(userId, "여행객" + userId, Role.TRAVELER);
    }

    // days 일짜리 일정을 가진 템플릿 (여행 기간 = 시작일 ~ 시작일 + days - 1)
    public void template(long id, long guideProfileId, int days) {
        jdbcTemplate.update("INSERT INTO template (id, created_at, modified_at, avg_rating, content, review_count, title, guide_profile_id) " +
                "VALUES (?, NOW(6), NOW(6), 0, '테스트 플랜 소개', 0, ?, ?)", id, "플랜" + id, guideProfileId);
        for (int day = 1; day <= days; day++) {
            jdbcTemplate.update("INSERT INTO template_itinerary (created_at, modified_at, content, day, latitude, location, longitude, start_time, template_id) " +
                    "VALUES (NOW(6), NOW(6), ?, ?, 37.5, ?, 127.0, '10:00:00', ?)", day + "일차", day, "장소" + day, id);
        }
    }

    public void trip(long id, long templateId, TripStatus status, LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update("INSERT INTO trip (id, created_at, modified_at, current_participants, start_date, end_date, max_participants, status, template_id) " +
                "VALUES (?, NOW(6), NOW(6), 0, ?, ?, 4, ?, ?)", id, startDate, endDate, status.name(), templateId);
    }

    public void room(long id, long tripId) {
        jdbcTemplate.update("INSERT INTO room (id, created_at, modified_at, trip_id) VALUES (?, NOW(6), NOW(6), ?)", id, tripId);
    }

    public void participation(long id, long tripId, long userId, Role role) {
        jdbcTemplate.update("INSERT INTO participation (id, created_at, modified_at, role, trip_id, user_id) " +
                "VALUES (?, NOW(6), NOW(6), ?, ?, ?)", id, role.name(), tripId, userId);
    }

    public void review(long id, long authorId, long tripId, long templateId, long guideProfileId) {
        jdbcTemplate.update("INSERT INTO review (id, created_at, modified_at, content, rating, author_id, guide_profile_id, trip_id, template_id) " +
                "VALUES (?, NOW(6), NOW(6), '좋은 여행이었어요', 5, ?, ?, ?, ?)", id, authorId, guideProfileId, tripId, templateId);
    }

    public void region(long id, String name) {
        jdbcTemplate.update("INSERT INTO region (id, level, name, parent_id) VALUES (?, 1, ?, NULL)", id, name);
    }

    public void tag(long id, String name) {
        jdbcTemplate.update("INSERT INTO tag (id, name) VALUES (?, ?)", id, name);
    }

    public void templateImage(long templateId, int displayOrder) {
        jdbcTemplate.update("INSERT INTO template_image (created_at, modified_at, display_order, image_url, template_id) " +
                "VALUES (NOW(6), NOW(6), ?, ?, ?)", displayOrder, "https://example.com/template" + templateId + "-" + displayOrder + ".png", templateId);
    }

    public void templateRegion(long templateId, long regionId) {
        jdbcTemplate.update("INSERT INTO template_region (template_id, region_id) VALUES (?, ?)", templateId, regionId);
    }

    public void templateTag(long templateId, long tagId) {
        jdbcTemplate.update("INSERT INTO template_tag (template_id, tag_id) VALUES (?, ?)", templateId, tagId);
    }

    public void wishlist(long userId, long tripId) {
        jdbcTemplate.update("INSERT INTO wishlist (created_at, modified_at, user_id, trip_id) VALUES (NOW(6), NOW(6), ?, ?)", userId, tripId);
    }

    // sender_role은 지금 ORDINAL로 저장된다 (D-14)
    public void chatMessage(long roomId, long senderId, Role senderRole) {
        jdbcTemplate.update("INSERT INTO chat_message (created_at, modified_at, content, sender_role, room_id, sender_id) " +
                "VALUES (NOW(6), NOW(6), '안녕하세요', ?, ?, ?)", senderRole.ordinal(), roomId, senderId);
    }

    public void userLocation(long userId, long roomId) {
        jdbcTemplate.update("INSERT INTO user_location (created_at, modified_at, latitude, longitude, user_id, room_id) " +
                "VALUES (NOW(6), NOW(6), 37.5, 127.0, ?, ?)", userId, roomId);
    }
}
