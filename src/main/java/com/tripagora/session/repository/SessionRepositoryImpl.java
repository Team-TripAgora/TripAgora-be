package com.tripagora.session.repository;

import com.tripagora.region.entity.QRegion;
import com.tripagora.session.dto.request.SessionSearchRequest;
import com.tripagora.session.entity.Session;
import com.tripagora.session.entity.SessionStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.tripagora.guideProfile.entity.QGuideProfile.guideProfile;
import static com.tripagora.region.entity.QRegion.region;
import static com.tripagora.region.entity.QTemplateRegion.templateRegion;
import static com.tripagora.session.entity.QSession.session;
import static com.tripagora.tag.entity.QTemplateTag.templateTag;
import static com.tripagora.template.entity.QTemplate.template;
import static com.tripagora.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryImpl implements SessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Session> search(SessionSearchRequest request, Pageable pageable) {
        QRegion parentRegion = new QRegion("parentRegion");

        // 1. 기본 쿼리 생성
        JPAQuery<Session> query = queryFactory
                .selectFrom(session)
                .join(session.template, template).fetchJoin()
                .join(template.guideProfile, guideProfile).fetchJoin()
                .join(guideProfile.user, user).fetchJoin()
                .leftJoin(template.templateRegions, templateRegion)
                .leftJoin(templateRegion.region, region)
                .leftJoin(region.parent, parentRegion); // [수정] 부모 지역(parent) 조인

        // 2. 동적 WHERE 조건 생성
        BooleanBuilder whereBuilder = buildDynamicWhere(request, parentRegion); // [수정] parentRegion 전달
        query.where(whereBuilder);

        // 3. 정렬 기능 제거, 기본 정렬(날짜 빠른 순)
        query.orderBy(session.startDate.asc());

        // 4. 중복 제거
        query.distinct();

        // 5. 페이징 (Slice 구현)
        query.offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1);

        List<Session> sessions = query.fetch();

        // 6. Slice 객체 생성
        boolean hasNext = false;
        if (sessions.size() > pageable.getPageSize()) {
            sessions.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(sessions, pageable, hasNext);
    }

    private BooleanBuilder buildDynamicWhere(SessionSearchRequest request, QRegion parentRegion) {
        BooleanBuilder builder = new BooleanBuilder();

        // 모집 중인 세션만 검색
        builder.and(session.status.eq(SessionStatus.RECRUITING));

        // --- 1. 검색어 (Keyword) ---
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword();
            builder.and(
                    template.title.containsIgnoreCase(keyword)
                            .or(template.content.containsIgnoreCase(keyword))
                            .or(user.nickname.containsIgnoreCase(keyword))
            );
        }

        // --- 2. 필터: 일정 (Date Range Overlap) ---
        if (request.getSearchStartDate() != null && request.getSearchEndDate() != null) {
            // (session.startDate <= request.endDate) AND (session.endDate >= request.startDate)
            builder.and(session.startDate.loe(request.getSearchEndDate())
                    .and(session.endDate.goe(request.getSearchStartDate())));
        } else if (request.getSearchStartDate() != null) {
            // 시작 날짜만 있는 경우: 그날 이후로 시작하는 모든 세션
            builder.and(session.startDate.goe(request.getSearchStartDate()));
        } else if (request.getSearchEndDate() != null) {
            // 종료 날짜만 있는 경우: 그날 이전에 끝나는 모든 세션 (필요시)
            builder.and(session.endDate.loe(request.getSearchEndDate()));
        }

        // --- 3. 필터: 지역 (regionIds) - (Req #4: Parent/Child logic) ---
        if (request.getRegionIds() != null && !request.getRegionIds().isEmpty()) {
            // 1. 해당 지역이거나 (e.g., 강남구 ID)
            // 2. 해당 지역의 부모가 검색 ID이거나 (e.g., 강남구의 부모 ID = 서울 ID)
            builder.and(
                    region.id.in(request.getRegionIds())
                            .or(parentRegion.id.in(request.getRegionIds()))
            );
        }

        // --- 4. 필터: 태그 (tagIds) ---
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            builder.and(template.id.in(
                    JPAExpressions
                            .select(templateTag.template.id)
                            .from(templateTag)
                            .where(templateTag.tag.id.in(request.getTagIds()))
            ));
        }

        return builder;
    }
}