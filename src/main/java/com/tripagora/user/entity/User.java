package com.tripagora.user.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.participation.entity.Participation;
import com.tripagora.review.entity.Review;
import com.tripagora.tag.entity.Tag;
import com.tripagora.tag.entity.UserTag;
import com.tripagora.wishlist.entity.Wishlist;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_social", columnNames = {"social_id", "social_type"})
        }
)

public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialType socialType;

    @Column(unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column
    private Role role;

    @Column(nullable = false)
    private String imageUrl;

    @Column
    private String fcmToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTag> userTags = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private GuideProfile guideProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participation> participations = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wishlist> wishlists = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @Builder
    private User(String socialId, SocialType socialType, String imageUrl) {
        this.socialId = socialId;
        this.socialType = socialType;
        this.imageUrl = imageUrl;
        this.role = Role.TRAVELER;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // 관심 태그를 tags로 바꾼다. 남는 태그는 그대로 두고, 빠진 태그는 컬렉션에서 빼고(orphanRemoval로 삭제), 새 태그만 추가한다
    public void replaceTags(List<Tag> tags) {
        Set<Long> newTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
        this.userTags.removeIf(userTag -> !newTagIds.contains(userTag.getTag().getId()));

        Set<Long> keptTagIds = this.userTags.stream()
                .map(userTag -> userTag.getTag().getId())
                .collect(Collectors.toSet());
        tags.stream()
                .filter(tag -> !keptTagIds.contains(tag.getId()))
                .forEach(tag -> this.userTags.add(UserTag.builder().user(this).tag(tag).build()));
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}