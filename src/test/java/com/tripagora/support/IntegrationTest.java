package com.tripagora.support;

import com.google.firebase.FirebaseApp;
import com.tripagora.auth.util.JWTUtil;
import com.tripagora.common.service.FcmService;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, DatabaseCleaner.class, QueryCounter.class, TestFixture.class})
public abstract class IntegrationTest {
    // FirebaseConfig 는 serviceAccountKey.json(git 제외)을 읽으므로 테스트에서는 대체한다
    @MockitoBean
    protected FirebaseApp firebaseApp;

    // 실제 FCM 전송을 하지 않는다
    @MockitoBean
    protected FcmService fcmService;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JWTUtil jwtUtil;

    @Autowired
    protected QueryCounter queryCounter;

    @Autowired
    protected TestFixture fixture;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    protected String bearer(long userId, Role role) {
        return "Bearer " + jwtUtil.createToken(userId, role.name(), true);
    }
}
