package com.tripagora.template.entity;

import com.tripagora.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "template_image")
public class TemplateImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(nullable = false)
    private String imageUrl;

    @Column
    private int displayOrder;

    @Builder
    private TemplateImage(Template template, String imageUrl, int displayOrder) {
        this.template = template;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}