package com.tripagora.support;

import com.amazonaws.services.s3.AmazonS3;
import com.google.firebase.FirebaseApp;
import com.tripagora.common.config.TimeConfig;
import com.tripagora.auth.util.JWTUtil;
import com.tripagora.common.service.FcmService;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, DatabaseCleaner.class, QueryCounter.class, TestFixture.class, TestClockConfig.class})
public abstract class IntegrationTest {
    // FirebaseConfig 는 serviceAccountKey.json(git 제외)을 읽으므로 테스트에서는 대체한다
    @MockitoBean
    protected FirebaseApp firebaseApp;

    // 실제 FCM 전송을 하지 않는다
    @MockitoBean
    protected FcmService fcmService;

    // 실제 S3에 올리거나 지우지 않는다
    @MockitoBean
    protected AmazonS3 amazonS3;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JWTUtil jwtUtil;

    @Autowired
    protected QueryCounter queryCounter;

    @Autowired
    protected TestFixture fixture;

    // 앱이 쓰는 시계. 테스트에서 시각을 고정할 수 있다 (끝나면 원래대로)
    @Autowired
    protected TestClock clock;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    // 앱과 같은 기준(한국 날짜)의 오늘. 테스트를 도는 JVM 시간대(CI는 UTC)와 상관없다
    protected static LocalDate today() {
        return LocalDate.now(TimeConfig.ZONE);
    }

    protected String bearer(long userId, Role role) {
        return "Bearer " + jwtUtil.createToken(userId, role.name(), true);
    }
}
