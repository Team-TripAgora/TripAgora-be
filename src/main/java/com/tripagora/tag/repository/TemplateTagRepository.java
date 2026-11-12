package com.tripagora.tag.repository;

import com.tripagora.tag.entity.TemplateTag;
import com.tripagora.tag.entity.TemplateTagId;
import com.tripagora.template.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateTagRepository extends JpaRepository<TemplateTag, TemplateTagId> {
    void deleteByTemplate(Template template);
}