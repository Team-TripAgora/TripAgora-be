package com.tripagora.region.repository;

import com.tripagora.region.entity.TemplateRegion;
import com.tripagora.region.entity.TemplateRegionId;
import com.tripagora.template.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRegionRepository extends JpaRepository<TemplateRegion, TemplateRegionId> {
    void deleteByTemplate(Template template);
}