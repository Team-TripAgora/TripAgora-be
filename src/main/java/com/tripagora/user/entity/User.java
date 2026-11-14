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
    // 탈퇴한 사용자는 소셜 id를 이 표식으로 바꿔 소셜 계정과의 연결을 끊는다 (같은 계정으로 다시 가입할 수 있게)
    private static final String WITHDRAWN_SOCIAL_ID_PREFIX = "withdrawn-";
    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

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

    // 탈퇴: 행은 남겨 남이 보는 참가·리뷰·채팅 이력을 지키고, 개인정보와 나만 보는 데이터만 지운다
    public void withdraw() {
        this.socialId = WITHDRAWN_SOCIAL_ID_PREFIX + this.id;
        this.nickname = null;
        this.imageUrl = "";
        this.fcmToken = null;
        this.userTags.clear();
        this.wishlists.clear();
        if (this.guideProfile != null) {
            this.guideProfile.clearPersonalInfo();
        }
    }

    public boolean isWithdrawn() {
        return this.socialId.startsWith(WITHDRAWN_SOCIAL_ID_PREFIX);
    }

    // 남에게 보이는 닉네임. 탈퇴한 사용자는 "탈퇴한 사용자"로 보인다 (닉네임 컬럼은 unique라 비워 둔다)
    public String getNickname() {
        return isWithdrawn() ? WITHDRAWN_NICKNAME : this.nickname;
    }
}