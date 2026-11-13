package com.tripagora.querycount;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 쿼리 수 기준선 테스트의 공통 부분. 동작 한 번에 실행된 SQL 수와 DB에서 불러온 엔티티 수를 잰다
// 숫자는 SQL 최적화 전 값이다. 쿼리 수가 바뀌는 변경을 하면 이유를 확인하고 숫자를 고친다
abstract class QueryCountTest extends IntegrationTest {
    protected final LocalDate today = LocalDate.now();

    @BeforeEach
    void createRegionsAndTags() {
        fixture.region(1L, "서울");
        fixture.region(2L, "부산");
        fixture.tag(1L, "맛집");
        fixture.tag(2L, "자연");
    }

    protected Counts measure(Action action) throws Exception {
        queryCounter.clear();
        action.run();
        return new Counts(queryCounter.count(), queryCounter.entityCount());
    }

    protected void assertCounts(Counts actual, long queries, long entities) {
        assertThat(actual).isEqualTo(new Counts(queries, entities));
    }

    // 여행마다 자기 템플릿을 하나씩 둔다. 템플릿 하나를 여러 여행이 같이 쓰면 영속성 컨텍스트 캐시 때문에 N+1이 가려진다
    // 템플릿마다 이미지 3장, 지역 2개, 태그 2개. 모집중이 아닌 여행에는 채팅방이 있다
    protected void tripWithOwnTemplate(long id, long guideProfileId, TripStatus status) {
        fixture.template(id, guideProfileId, 2);
        for (int order = 0; order < 3; order++) {
            fixture.templateImage(id, order);
        }
        fixture.templateRegion(id, 1L);
        fixture.templateRegion(id, 2L);
        fixture.templateTag(id, 1L);
        fixture.templateTag(id, 2L);

        LocalDate startDate = today.plusDays(30 + id * 3);
        fixture.trip(id, id, status, startDate, startDate.plusDays(1));
        if (status != TripStatus.RECRUITING) {
            fixture.room(id, id);
        }
    }

    protected record Counts(long queries, long entities) {
    }

    @FunctionalInterface
    protected interface Action {
        void run() throws Exception;
    }
}
