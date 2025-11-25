package com.example.TripAgora.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -1106281249L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final com.example.TripAgora.common.entity.QBaseEntity _super = new com.example.TripAgora.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath fcmToken = createString("fcmToken");

    public final com.example.TripAgora.guideProfile.entity.QGuideProfile guideProfile;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath nickname = createString("nickname");

    public final ListPath<com.example.TripAgora.participation.entity.Participation, com.example.TripAgora.participation.entity.QParticipation> participations = this.<com.example.TripAgora.participation.entity.Participation, com.example.TripAgora.participation.entity.QParticipation>createList("participations", com.example.TripAgora.participation.entity.Participation.class, com.example.TripAgora.participation.entity.QParticipation.class, PathInits.DIRECT2);

    public final ListPath<com.example.TripAgora.review.entity.Review, com.example.TripAgora.review.entity.QReview> reviews = this.<com.example.TripAgora.review.entity.Review, com.example.TripAgora.review.entity.QReview>createList("reviews", com.example.TripAgora.review.entity.Review.class, com.example.TripAgora.review.entity.QReview.class, PathInits.DIRECT2);

    public final EnumPath<Role> role = createEnum("role", Role.class);

    public final StringPath socialId = createString("socialId");

    public final EnumPath<SocialType> socialType = createEnum("socialType", SocialType.class);

    public final ListPath<com.example.TripAgora.tag.entity.UserTag, com.example.TripAgora.tag.entity.QUserTag> userTags = this.<com.example.TripAgora.tag.entity.UserTag, com.example.TripAgora.tag.entity.QUserTag>createList("userTags", com.example.TripAgora.tag.entity.UserTag.class, com.example.TripAgora.tag.entity.QUserTag.class, PathInits.DIRECT2);

    public final ListPath<com.example.TripAgora.wishlist.entity.Wishlist, com.example.TripAgora.wishlist.entity.QWishlist> wishlists = this.<com.example.TripAgora.wishlist.entity.Wishlist, com.example.TripAgora.wishlist.entity.QWishlist>createList("wishlists", com.example.TripAgora.wishlist.entity.Wishlist.class, com.example.TripAgora.wishlist.entity.QWishlist.class, PathInits.DIRECT2);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.guideProfile = inits.isInitialized("guideProfile") ? new com.example.TripAgora.guideProfile.entity.QGuideProfile(forProperty("guideProfile"), inits.get("guideProfile")) : null;
    }

}

