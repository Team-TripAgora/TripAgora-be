package com.example.TripAgora.session.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSession is a Querydsl query type for Session
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSession extends EntityPathBase<Session> {

    private static final long serialVersionUID = 21174319L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSession session = new QSession("session");

    public final com.example.TripAgora.common.entity.QBaseEntity _super = new com.example.TripAgora.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> currentParticipants = createNumber("currentParticipants", Integer.class);

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> maxParticipants = createNumber("maxParticipants", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final ListPath<com.example.TripAgora.participation.entity.Participation, com.example.TripAgora.participation.entity.QParticipation> participants = this.<com.example.TripAgora.participation.entity.Participation, com.example.TripAgora.participation.entity.QParticipation>createList("participants", com.example.TripAgora.participation.entity.Participation.class, com.example.TripAgora.participation.entity.QParticipation.class, PathInits.DIRECT2);

    public final ListPath<com.example.TripAgora.review.entity.Review, com.example.TripAgora.review.entity.QReview> reviews = this.<com.example.TripAgora.review.entity.Review, com.example.TripAgora.review.entity.QReview>createList("reviews", com.example.TripAgora.review.entity.Review.class, com.example.TripAgora.review.entity.QReview.class, PathInits.DIRECT2);

    public final com.example.TripAgora.room.entity.QRoom room;

    public final ListPath<SessionItinerary, QSessionItinerary> sessionItineraries = this.<SessionItinerary, QSessionItinerary>createList("sessionItineraries", SessionItinerary.class, QSessionItinerary.class, PathInits.DIRECT2);

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final EnumPath<SessionStatus> status = createEnum("status", SessionStatus.class);

    public final com.example.TripAgora.template.entity.QTemplate template;

    public final ListPath<com.example.TripAgora.wishlist.entity.Wishlist, com.example.TripAgora.wishlist.entity.QWishlist> wishlists = this.<com.example.TripAgora.wishlist.entity.Wishlist, com.example.TripAgora.wishlist.entity.QWishlist>createList("wishlists", com.example.TripAgora.wishlist.entity.Wishlist.class, com.example.TripAgora.wishlist.entity.QWishlist.class, PathInits.DIRECT2);

    public QSession(String variable) {
        this(Session.class, forVariable(variable), INITS);
    }

    public QSession(Path<? extends Session> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSession(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSession(PathMetadata metadata, PathInits inits) {
        this(Session.class, metadata, inits);
    }

    public QSession(Class<? extends Session> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new com.example.TripAgora.room.entity.QRoom(forProperty("room"), inits.get("room")) : null;
        this.template = inits.isInitialized("template") ? new com.example.TripAgora.template.entity.QTemplate(forProperty("template"), inits.get("template")) : null;
    }

}

