package com.tripagora.region.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class TemplateRegionId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "region_id")
    private Long regionId;

    public TemplateRegionId(Long templateId, Long regionId) {
        this.templateId = templateId;
        this.regionId = regionId;
    }
}