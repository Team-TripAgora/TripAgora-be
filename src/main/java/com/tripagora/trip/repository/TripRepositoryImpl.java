package com.tripagora.trip.repository;

import com.tripagora.region.entity.QRegion;
import com.tripagora.trip.dto.request.TripSearchRequest;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripStatus;
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

import java.time.LocalDate;
import java.util.List;

import static com.tripagora.guideprofile.entity.QGuideProfile.guideProfile;
import static com.tripagora.region.entity.QRegion.region;
import static com.tripagora.region.entity.QTemplateRegion.templateRegion;
import static com.tripagora.trip.entity.QTrip.trip;
import static com.tripagora.tag.entity.QTemplateTag.templateTag;
import static com.tripagora.template.entity.QTemplate.template;
import static com.tripagora.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class TripRepositoryImpl implements TripRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Trip> search(TripSearchRequest request, LocalDate today, Pageable pageable) {
        QRegion parentRegion = new QRegion("parentRegion");

        // 검색 조건(검색어·지역)이 템플릿·가이드·지역을 쓴다. 모집 중 여행은 템플릿을 고칠 수 없어 여행 사본과 내용이 같다
        JPAQuery<Trip> query = queryFactory
                .selectFrom(trip)
                .join(trip.template, template).fetchJoin()
                .join(template.guideProfile, guideProfile).fetchJoin()
                .join(guideProfile.user, user).fetchJoin()
                .leftJoin(template.templateRegions, templateRegion)
                .leftJoin(templateRegion.region, region)
                .leftJoin(region.parent, parentRegion); // 상위 지역으로 검색해도 하위 지역 여행이 나오게

        query.where(buildDynamicWhere(request, today, parentRegion));

        // 시작일이 빠른 순. 시작일이 같으면 id 순으로 고정한다 (정렬 값이 같은 줄의 순서는 DB가 정하지 않아, 페이지 경계에서 빠지거나 겹칠 수 있다)
        query.orderBy(trip.startDate.asc(), trip.id.asc());

        // 지역 조인으로 같은 여행이 여러 줄이 되므로 중복을 없앤다
        query.distinct();

        // 다음 페이지가 있는지 보려고 한 건 더 읽는다
        query.offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1);

        List<Trip> trips = query.fetch();

        boolean hasNext = false;
        if (trips.size() > pageable.getPageSize()) {
            trips.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(trips, pageable, hasNext);
    }

    private BooleanBuilder buildDynamicWhere(TripSearchRequest request, LocalDate today, QRegion parentRegion) {
        BooleanBuilder builder = new BooleanBuilder();

        // 모집 중이고 아직 시작하지 않은 여행만 (모집은 시작 전날까지)
        builder.and(trip.status.eq(TripStatus.RECRUITING));
        builder.and(trip.startDate.gt(today));

        // 검색어: 제목·소개·가이드 닉네임
        if (StringUtils.hasText(request.keyword())) {
            String keyword = request.keyword();
            builder.and(
                    template.title.containsIgnoreCase(keyword)
                            .or(template.content.containsIgnoreCase(keyword))
                            .or(user.nickname.containsIgnoreCase(keyword))
            );
        }

        // 일정: 검색 기간과 겹치는 여행 (시작일 <= 검색 끝, 종료일 >= 검색 시작)
        if (request.searchStartDate() != null && request.searchEndDate() != null) {
            builder.and(trip.startDate.loe(request.searchEndDate())
                    .and(trip.endDate.goe(request.searchStartDate())));
        } else if (request.searchStartDate() != null) {
            // 시작 날짜만 있는 경우: 그날 이후로 시작하는 모든 여행
            builder.and(trip.startDate.goe(request.searchStartDate()));
        } else if (request.searchEndDate() != null) {
            // 종료 날짜만 있는 경우: 그날 이전에 끝나는 모든 여행
            builder.and(trip.endDate.loe(request.searchEndDate()));
        }

        // 지역: 그 지역이거나, 그 지역의 하위 지역 (예: 서울로 검색하면 강남구 여행도)
        if (request.regionIds() != null && !request.regionIds().isEmpty()) {
            builder.and(
                    region.id.in(request.regionIds())
                            .or(parentRegion.id.in(request.regionIds()))
            );
        }

        // 태그: 템플릿에 그 태그 중 하나라도 있는 여행
        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            builder.and(template.id.in(
                    JPAExpressions
                            .select(templateTag.template.id)
                            .from(templateTag)
                            .where(templateTag.tag.id.in(request.tagIds()))
            ));
        }

        return builder;
    }
}