package com.tripagora.template.service;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.common.image.service.ImageService;
import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.guideprofile.exception.GuideProfileNotFoundException;
import com.tripagora.region.entity.Region;
import com.tripagora.region.entity.TemplateRegion;
import com.tripagora.region.exception.InvalidRegionSelectionException;
import com.tripagora.region.exception.RegionHierarchyConflictException;
import com.tripagora.region.repository.RegionRepository;
import com.tripagora.region.repository.TemplateRegionRepository;
import com.tripagora.session.repository.SessionRepository;
import com.tripagora.tag.entity.Tag;
import com.tripagora.tag.entity.TemplateTag;
import com.tripagora.tag.exception.InvalidTagSelectionException;
import com.tripagora.tag.exception.MaximumTagsExceededException;
import com.tripagora.tag.repository.TagRepository;
import com.tripagora.tag.repository.TemplateTagRepository;
import com.tripagora.template.dto.request.*;
import com.tripagora.template.dto.response.*;
import com.tripagora.template.entity.Template;
import com.tripagora.template.entity.TemplateImage;
import com.tripagora.template.exception.ItineraryDaySequenceInvalidException;
import com.tripagora.template.exception.TemplateHasActiveSessionsException;
import com.tripagora.template.exception.TemplateNotFoundException;
import com.tripagora.template.repository.TemplateRepository;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final ImageService imageService;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final RegionRepository regionRepository;
    private final TemplateTagRepository templateTagRepository;
    private final TemplateRegionRepository templateRegionRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public TemplateCreateResponse createDraftTemplate(long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        GuideProfile guideProfile = user.getGuideProfile();

        if (guideProfile == null) {
            throw new GuideProfileNotFoundException();
        }

        Template draftTemplate = Template.builder()
                .guideProfile(guideProfile)
                .build();

        Template savedTemplate = templateRepository.save(draftTemplate);
        return new TemplateCreateResponse(savedTemplate.getId());
    }

    @Transactional(readOnly = true)
    public TemplateDetailResponse getTemplate(long userId, long templateId) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);

        List<Long> regionIds = template.getTemplateRegions().stream()
                .map(templateRegion -> templateRegion.getRegion().getId())
                .collect(Collectors.toList());

        List<Long> tagIds = template.getTemplateTags().stream()
                .map(templateTag -> templateTag.getTag().getId())
                .collect(Collectors.toList());

        List<String> imageUrls = template.getTemplateImages().stream()
                .map(TemplateImage::getImageUrl)
                .toList();

        return new TemplateDetailResponse(
                template.getTitle(),
                template.getContent(),
                regionIds,
                tagIds,
                imageUrls);
    }

    @Transactional(readOnly = true)
    public TemplateListResponse getMyTemplates(long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        GuideProfile guideProfile = user.getGuideProfile();
        if (guideProfile == null) {
            throw new GuideProfileNotFoundException();
        }

        Slice<Template> templateSlice = templateRepository.findByGuideProfileOrderByIdDesc(guideProfile, pageable);

        List<TemplateSummaryResponse> summaries = templateSlice.getContent().stream()
                .map(template -> {
                    String representativeImageUrl = template.getTemplateImages().stream()
                            .filter(image -> image.getDisplayOrder() == 1)
                            .findFirst()
                            .map(TemplateImage::getImageUrl)
                            .orElse(null);

                    List<Long> regionIds = template.getTemplateRegions().stream()
                            .map(templateRegion -> templateRegion.getRegion().getId())
                            .collect(Collectors.toList());

                    return new TemplateSummaryResponse(
                            template.getId(),
                            template.getTitle(),
                            representativeImageUrl,
                            regionIds);
                }).collect(Collectors.toList());

        return new TemplateListResponse(summaries, templateSlice.hasNext());
    }

    @Transactional(readOnly = true)
    public ItinerariesResponse getItineraries(long userId, long templateId) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);

        List<ItineraryItemResponse> itineraries = template.getTemplateItineraries().stream()
                .map(itinerary -> new ItineraryItemResponse(
                        itinerary.getId(),
                        itinerary.getDay(),
                        itinerary.getLocation(),
                        itinerary.getContent(),
                        itinerary.getStartTime(),
                        itinerary.getLatitude(),
                        itinerary.getLongitude()))
                .toList();

        return new ItinerariesResponse(itineraries);
    }

    @Transactional
    public TemplateTitleUpdateResponse updateTitle(long userId, long templateId, TemplateTitleUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveSessions(templateId);
        template.updateTitle(request.title());

        return new TemplateTitleUpdateResponse(template.getTitle());
    }

    @Transactional
    public TemplateContentUpdateResponse updateContent(long userId, long templateId, TemplateContentUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveSessions(templateId);
        template.updateContent(request.content());

        return new TemplateContentUpdateResponse(template.getContent());
    }

    @Transactional
    public TemplateImageUpdateResponse updateImages(long userId, long templateId, List<MultipartFile> images) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveSessions(templateId);

        template.getTemplateImages().forEach(image -> imageService.deleteImage(image.getImageUrl()));
        template.clearImages();

        List<String> newImageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            images.forEach(image -> {
                String imageUrl = imageService.uploadImage(image);
                template.addImage(imageUrl);
                newImageUrls.add(imageUrl);
            });
        }

        return new TemplateImageUpdateResponse(newImageUrls);
    }

    @Transactional
    public TemplateTagUpdateResponse updateTags(long userId, long templateId, TemplateTagUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveSessions(templateId);

        List<Long> tagIds = request.tagIds();

        if (tagIds.size() > 5) {
            throw new MaximumTagsExceededException();
        }

        List<Tag> newTags = tagRepository.findAllById(tagIds);
        if (newTags.size() != tagIds.size()) {
            throw new InvalidTagSelectionException();
        }

        templateTagRepository.deleteByTemplate(template);
        List<TemplateTag> newUserTags = newTags.stream()
                .map(tag -> TemplateTag.builder()
                        .template(template)
                        .tag(tag)
                        .build())
                .toList();
        templateTagRepository.saveAll(newUserTags);

        List<Long> newTagIds = newTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toList());

        return new TemplateTagUpdateResponse(newTagIds);
    }

    @Transactional
    public TemplateRegionUpdateResponse updateRegions(long userId, long templateId, TemplateRegionUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveSessions(templateId);

        List<Long> regionIds = request.regionIds();

        List<Region> newRegions = regionRepository.findAllById(regionIds);
        if (newRegions.size() != regionIds.size()) {
            throw new InvalidRegionSelectionException();
        }

        for (Region region : newRegions) {
            if (region.getParent() != null && newRegions.contains(region.getParent())) {
                throw new RegionHierarchyConflictException();
            }
        }

        templateRegionRepository.deleteByTemplate(template);
        List<TemplateRegion> newTemplateRegions = newRegions.stream()
                .map(region -> TemplateRegion.builder()
                        .template(template)
                        .region(region)
                        .build())
                .toList();
        templateRegionRepository.saveAll(newTemplateRegions);

        List<Long> newRegionIds = newRegions.stream()
                .map(Region::getId)
                .collect(Collectors.toList());

        return new TemplateRegionUpdateResponse(newRegionIds);
    }

    @Transactional
    public void updateItineraries(long userId, long templateId, ItineraryUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveSessions(templateId);

        Map<Integer, List<ItineraryItemRequest>> itinerariesByDay = request.itineraries().stream()
                .collect(Collectors.groupingBy(ItineraryItemRequest::day));

        int maxDay = itinerariesByDay.keySet().stream().max(Integer::compareTo).orElse(0);
        for (int i = 1; i <= maxDay; i++) {
            if (!itinerariesByDay.containsKey(i)) {
                throw new ItineraryDaySequenceInvalidException();
            }
        }

        template.clearItineraries();
        request.itineraries().forEach(template::addItinerary);
    }

    @Transactional
    public void deleteTemplate(long userId, long templateId) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveSessions(templateId);

        templateRepository.delete(template);
    }

    public Template findTemplateAndVerifyOwner(long userId, long templateId) {
        Template template = templateRepository.findById(templateId).orElseThrow(TemplateNotFoundException::new);

        if (!template.getGuideProfile().getUser().getId().equals(userId)) {
            throw new AccessDeniedException();
        }

        return template;
    }

    private void verifyNoActiveSessions(long templateId) {
        if (sessionRepository.existsActiveSessionByTemplateId(templateId)) {
            throw new TemplateHasActiveSessionsException();
        }
    }
}