package com.tripagora.template.service;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.common.image.event.ImageDeletionEvent;
import com.tripagora.common.image.service.ImageService;
import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.guideprofile.exception.GuideProfileNotFoundException;
import com.tripagora.region.entity.Region;
import com.tripagora.region.exception.InvalidRegionSelectionException;
import com.tripagora.region.exception.RegionHierarchyConflictException;
import com.tripagora.region.repository.RegionRepository;
import com.tripagora.trip.repository.TripRepository;
import com.tripagora.tag.entity.Tag;
import com.tripagora.tag.exception.InvalidTagSelectionException;
import com.tripagora.tag.exception.MaximumTagsExceededException;
import com.tripagora.tag.repository.TagRepository;
import com.tripagora.template.dto.request.*;
import com.tripagora.template.dto.response.*;
import com.tripagora.template.entity.Template;
import com.tripagora.template.entity.TemplateImage;
import com.tripagora.template.exception.ItineraryDaySequenceInvalidException;
import com.tripagora.template.exception.TemplateHasActiveTripsException;
import com.tripagora.template.exception.TemplateNotFoundException;
import com.tripagora.template.repository.TemplateRepository;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final TripRepository tripRepository;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

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
        verifyNoActiveTrips(templateId);
        template.updateTitle(request.title());

        return new TemplateTitleUpdateResponse(template.getTitle());
    }

    @Transactional
    public TemplateContentUpdateResponse updateContent(long userId, long templateId, TemplateContentUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveTrips(templateId);
        template.updateContent(request.content());

        return new TemplateContentUpdateResponse(template.getContent());
    }

    // S3 업로드 동안 DB 트랜잭션을 열어 두지 않는다: 권한 확인(짧은 트랜잭션) → 새 파일 업로드(트랜잭션 밖) → DB 교체(짧은 트랜잭션).
    // 원본 파일은 DB가 커밋된 뒤에만 지우고, DB 교체가 실패하면 방금 올린 파일을 지운다
    public TemplateImageUpdateResponse updateImages(long userId, long templateId, List<MultipartFile> images) {
        transactionTemplate.executeWithoutResult(status -> {
            findTemplateAndVerifyOwner(userId, templateId);
            verifyNoActiveTrips(templateId);
        });

        List<String> newImageUrls = uploadImages(images);
        try {
            transactionTemplate.executeWithoutResult(status -> replaceImages(userId, templateId, newImageUrls));
        } catch (RuntimeException e) {
            imageService.deleteImagesQuietly(newImageUrls);
            throw e;
        }

        return new TemplateImageUpdateResponse(newImageUrls);
    }

    private List<String> uploadImages(List<MultipartFile> images) {
        List<String> uploadedImageUrls = new ArrayList<>();
        if (images == null) {
            return uploadedImageUrls;
        }
        try {
            images.forEach(image -> uploadedImageUrls.add(imageService.uploadImage(image)));
        } catch (RuntimeException e) {
            imageService.deleteImagesQuietly(uploadedImageUrls);
            throw e;
        }
        return uploadedImageUrls;
    }

    private void replaceImages(long userId, long templateId, List<String> newImageUrls) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveTrips(templateId);

        List<String> oldImageUrls = template.getTemplateImages().stream()
                .map(TemplateImage::getImageUrl)
                .toList();
        template.clearImages();
        newImageUrls.forEach(template::addImage);

        eventPublisher.publishEvent(new ImageDeletionEvent(oldImageUrls));
    }

    @Transactional
    public TemplateTagUpdateResponse updateTags(long userId, long templateId, TemplateTagUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveTrips(templateId);

        List<Long> tagIds = request.tagIds();

        if (tagIds.size() > 5) {
            throw new MaximumTagsExceededException();
        }

        List<Tag> newTags = tagRepository.findAllById(tagIds);
        if (newTags.size() != tagIds.size()) {
            throw new InvalidTagSelectionException();
        }

        template.replaceTags(newTags);

        List<Long> newTagIds = newTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toList());

        return new TemplateTagUpdateResponse(newTagIds);
    }

    @Transactional
    public TemplateRegionUpdateResponse updateRegions(long userId, long templateId, TemplateRegionUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveTrips(templateId);

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

        template.replaceRegions(newRegions);

        List<Long> newRegionIds = newRegions.stream()
                .map(Region::getId)
                .collect(Collectors.toList());

        return new TemplateRegionUpdateResponse(newRegionIds);
    }

    @Transactional
    public void updateItineraries(long userId, long templateId, ItineraryUpdateRequest request) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        verifyNoActiveTrips(templateId);

        Map<Integer, List<ItineraryItemRequest>> itinerariesByDay = request.itineraries().stream()
                .collect(Collectors.groupingBy(ItineraryItemRequest::day));

        int maxDay = itinerariesByDay.keySet().stream().max(Integer::compareTo).orElse(0);
        for (int i = 1; i <= maxDay; i++) {
            if (!itinerariesByDay.containsKey(i)) {
                throw new ItineraryDaySequenceInvalidException();
            }
        }

        template.clearItineraries();
        request.itineraries().forEach(item -> template.addItinerary(item.toTemplateItinerary(template)));
    }

    @Transactional
    public void deleteTemplate(long userId, long templateId) {
        Template template = findTemplateAndVerifyOwner(userId, templateId);
        // 여행을 한 번이라도 연 템플릿은 지우지 않는다. 지우면 그 여행의 참가·리뷰·채팅 이력이 함께 사라진다 (완료된 여행 포함)
        if (tripRepository.existsByTemplate_Id(templateId)) {
            throw new TemplateHasActiveTripsException();
        }

        templateRepository.delete(template);
    }

    public Template findTemplateAndVerifyOwner(long userId, long templateId) {
        Template template = templateRepository.findById(templateId).orElseThrow(TemplateNotFoundException::new);

        if (!template.getGuideProfile().getUser().getId().equals(userId)) {
            throw new AccessDeniedException();
        }

        return template;
    }

    private void verifyNoActiveTrips(long templateId) {
        if (tripRepository.existsActiveTripByTemplateId(templateId)) {
            throw new TemplateHasActiveTripsException();
        }
    }
}