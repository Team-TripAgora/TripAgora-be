package com.tripagora.region.entity;

import com.tripagora.template.entity.Template;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "template_region")
public class TemplateRegion {
    @EmbeddedId
    private TemplateRegionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("templateId")
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("regionId")
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Builder
    private TemplateRegion(Template template, Region region) {
        this.template = template;
        this.region = region;
        this.id = new TemplateRegionId(template.getId(), region.getId());
    }
}