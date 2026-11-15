package com.tripagora.template;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URL;
import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 템플릿과 여행 사본은 같은 S3 사진 파일을 함께 쓴다.
// 템플릿 사진을 바꿀 때 여행 사본이 아직 쓰는 파일은 지우지 않고, 아무도 안 쓰는 옛 파일만 지운다
class TemplateImageSharedWithTripTest extends IntegrationTest {
    private static final String BUCKET = "test-bucket";
    private static final String S3_URL = "https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/";
    private static final String USED_BY_TRIP_KEY = "images/used-by-trip.jpg";
    private static final String UNUSED_KEY = "images/unused.jpg";
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;
    private static final long COMPLETED_TRIP = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDate lastWeek = today().minusDays(7);
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 1);
        templateImage(USED_BY_TRIP_KEY, 1);
        // 이 사진이 있을 때 연 여행 (사본이 같은 파일 주소를 갖는다)
        fixture.trip(COMPLETED_TRIP, TEMPLATE, TripStatus.COMPLETED, lastWeek, lastWeek);
        // 여행이 끝난 뒤 템플릿에 추가한 사진 (어느 여행도 쓰지 않는다)
        templateImage(UNUSED_KEY, 2);
        when(amazonS3.getUrl(anyString(), anyString())).thenAnswer(invocation -> new URL(S3_URL + invocation.getArgument(1)));
    }

    @Test
    @DisplayName("템플릿 사진을 바꾸면 여행 사본이 쓰는 옛 파일은 남기고, 아무도 안 쓰는 옛 파일만 지운다")
    void keepsFilesUsedByTripSnapshots() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/templates/" + TEMPLATE + "/images")
                        .file(new MockMultipartFile("images", "new.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk());

        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, UNUSED_KEY);
        verify(amazonS3, after(1_000).never()).deleteObject(BUCKET, USED_BY_TRIP_KEY);
        mockMvc.perform(get("/api/trips/" + COMPLETED_TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.imageUrls", contains(S3_URL + USED_BY_TRIP_KEY)));
    }

    private void templateImage(String key, int displayOrder) {
        jdbcTemplate.update("INSERT INTO template_image (created_at, modified_at, display_order, image_url, template_id) " +
                "VALUES (NOW(6), NOW(6), ?, ?, ?)", displayOrder, S3_URL + key, TEMPLATE);
    }
}
