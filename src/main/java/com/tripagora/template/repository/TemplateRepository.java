package com.tripagora.template.repository;

import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.template.entity.Template;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    Slice<Template> findByGuideProfileOrderByIdDesc(GuideProfile guideProfile, Pageable pageable);
    List<Template> findByGuideProfile(GuideProfile guideProfile);
}