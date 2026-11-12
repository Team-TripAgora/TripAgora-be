package com.tripagora.tag.entity;

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
public class TemplateTagId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "tag_id")
    private Long tagId;

    public TemplateTagId(Long templateId, Long tagId) {
        this.templateId = templateId;
        this.tagId = tagId;
    }
}