package com.tripagora.querycount;

import com.tripagora.chat.dto.request.ChatMessageRequest;
import com.tripagora.chat.service.ChatService;
import com.tripagora.location.dto.request.LocationUpdateRequest;
import com.tripagora.location.service.LocationService;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 채팅·위치의 쿼리 수 기준선
// STOMP SEND는 서비스 메서드를 직접 부른다. SEND 때 StompAuthInterceptor는 DB를 조회하지 않고,
// 컨트롤러는 서비스 결과를 브로커로 보내기만 하므로 서비스 호출이 SEND 1건의 DB 작업 전부다
class ChatLocationQueryCountTest extends QueryCountTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long ROOM = 1L;
    private static final int MESSAGES = 30;

    @Autowired
    private ChatService chatService;

    @Autowired
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        tripWithOwnTemplate(1L, GUIDE, TripStatus.RECRUITMENT_CLOSED);
        fixture.participation(GUIDE, 1L, GUIDE, Role.GUIDE);
    }

    // 가이드(1번)와 여행객(2번부터)을 합쳐 participants 명이 참가한다
    private void travelersUpTo(int participants) {
        for (long userId = TRAVELER; userId <= participants; userId++) {
            fixture.traveler(userId);
            fixture.participation(userId, 1L, userId, Role.TRAVELER);
        }
    }

    @ParameterizedTest(name = "{displayName}: 보낸 사람 {0}명 → 쿼리 {1}개, 엔티티 {2}개")
    @CsvSource({"2, 5, 35", "4, 7, 37"})
    @DisplayName("채팅 이력 (메시지 30건)")
    void chatHistory(int senders, long queries, long entities) throws Exception {
        travelersUpTo(senders);
        for (int i = 0; i < MESSAGES; i++) {
            long senderId = 1 + (i % senders);
            fixture.chatMessage(ROOM, senderId, senderId == GUIDE ? Role.GUIDE : Role.TRAVELER);
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/rooms/" + ROOM + "/messages").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(MESSAGES))));

        assertCounts(counts, queries, entities);
    }

    @Test
    @DisplayName("채팅 SEND 1건")
    void sendChat() throws Exception {
        travelersUpTo(2);

        Counts counts = measure(() -> chatService.processAndSaveMessage(TRAVELER, ROOM, new ChatMessageRequest("안녕하세요")));

        assertCounts(counts, 5, 4);
    }

    @Test
    @DisplayName("위치 SEND 1건 (처음 보냄 → 저장)")
    void sendFirstLocation() throws Exception {
        travelersUpTo(2);

        Counts counts = measure(() -> locationService.updateLocation(TRAVELER, ROOM, new LocationUpdateRequest(37.51, 127.01)));

        assertCounts(counts, 6, 4);
    }

    @Test
    @DisplayName("위치 SEND 1건 (저장된 위치 있음 → 갱신)")
    void sendUpdatedLocation() throws Exception {
        travelersUpTo(2);
        fixture.userLocation(TRAVELER, ROOM);

        Counts counts = measure(() -> locationService.updateLocation(TRAVELER, ROOM, new LocationUpdateRequest(37.51, 127.01)));

        assertCounts(counts, 6, 5);
    }

    @ParameterizedTest(name = "{displayName}: 위치 {0}명 → 쿼리 {1}개, 엔티티 {2}개")
    @CsvSource({"2, 5, 7", "4, 7, 11"})
    @DisplayName("위치 목록")
    void roomLocations(int participants, long queries, long entities) throws Exception {
        travelersUpTo(participants);
        for (long userId = GUIDE; userId <= participants; userId++) {
            fixture.userLocation(userId, ROOM);
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/rooms/" + ROOM + "/locations").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(participants))));

        assertCounts(counts, queries, entities);
    }
}
