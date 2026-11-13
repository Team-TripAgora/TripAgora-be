package com.tripagora.guideprofile.service;

import com.tripagora.auth.dto.ReissueResponse;
import com.tripagora.auth.service.JWTService;
import com.tripagora.common.image.service.ImageService;
import com.tripagora.guideprofile.dto.request.GuideBioUpdateRequest;
import com.tripagora.guideprofile.dto.request.GuidePortfolioUpdateRequest;
import com.tripagora.guideprofile.dto.response.*;
import com.tripagora.guideprofile.entity.GuidePortfolio;
import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.guideprofile.exception.AlreadyGuideException;
import com.tripagora.guideprofile.exception.AlreadyTravelerException;
import com.tripagora.guideprofile.exception.GuideProfileNotFoundException;
import com.tripagora.guideprofile.repository.GuideProfileRepository;
import com.tripagora.trip.dto.response.TripListResponse;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.repository.TripRepository;
import com.tripagora.trip.service.TripService;
import com.tripagora.user.dto.response.GuideSwitchResponse;
import com.tripagora.user.dto.response.TravelerSwitchResponse;
import com.tripagora.user.entity.Role;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuideProfileService {
    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final GuideProfileRepository guideProfileRepository;
    private final TripRepository tripRepository;
    private final ImageService imageService;

    @Transactional
    public GuideSwitchResponse switchToGuide(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getRole() == Role.GUIDE) {
            throw new AlreadyGuideException();
        }

        GuideProfile profile = null;
        if (user.getGuideProfile() == null) {
            GuideProfile newProfile = GuideProfile.builder()
                    .user(user)
                    .build();
            profile = guideProfileRepository.save(newProfile);
        } else {
            profile = user.getGuideProfile();
        }

        user.updateRole(Role.GUIDE);

        ReissueResponse tokenResponse = jwtService.issueTokensForUser(user);
        return new GuideSwitchResponse(tokenResponse.accessToken(), tokenResponse.refreshToken(), profile.getId());
    }

    @Transactional
    public TravelerSwitchResponse switchToTraveler(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getRole() == Role.TRAVELER) {
            throw new AlreadyTravelerException();
        }

        user.updateRole(Role.TRAVELER);
        ReissueResponse tokenResponse = jwtService.issueTokensForUser(user);
        return new TravelerSwitchResponse(tokenResponse.accessToken(), tokenResponse.refreshToken(), user.getId());
    }

    @Transactional(readOnly = true)
    public GuideProfileDetailResponse getMyGuideProfileDetails(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        GuideProfile guideProfile = user.getGuideProfile();
        if (guideProfile == null) {
            throw new GuideProfileNotFoundException();
        }

        return getGuideProfileDetails(guideProfile.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public GuideProfileDetailResponse getGuideProfileDetails(Long guideProfileId, Pageable pageable) {
        GuideProfile guideProfile = guideProfileRepository.findById(guideProfileId).orElseThrow(GuideProfileNotFoundException::new);
        User user = guideProfile.getUser();

        List<Long> tagIds = user.getUserTags().stream()
                .map(templateTag -> templateTag.getTag().getId())
                .toList();

        List<PortfolioResponse> portfolios = guideProfile.getPortfolios().stream()
                .map(portfolio -> new PortfolioResponse(
                        portfolio.getType().name(),
                        portfolio.getUrl()))
                .collect(Collectors.toList());

        List<TripStatus> statuses = new ArrayList<>();
        statuses.add(TripStatus.RECRUITING);
        Slice<Trip> tripSlice = tripRepository.findByTemplate_GuideProfileAndStatusIn(guideProfile, statuses, pageable);

        TripListResponse tripListResponse = TripService.getTripListResponse(tripSlice);

        return new GuideProfileDetailResponse(
                user.getNickname(),
                user.getImageUrl(),
                guideProfile.getImageUrl(),
                guideProfile.getBio(),
                tagIds,
                portfolios,
                tripListResponse
        );
    }

    @Transactional
    public GuideBioUpdateResponse updateMyBio(Long userId, GuideBioUpdateRequest request) {
        GuideProfile guideProfile = guideProfileRepository.findByUser_Id(userId).orElseThrow(GuideProfileNotFoundException::new);
        guideProfile.updateBio(request.bio());

        return new GuideBioUpdateResponse(guideProfile.getBio());
    }

    @Transactional
    public GuideImageUpdateResponse updateMyImageUrl(Long userId, MultipartFile imageFile) {
        GuideProfile guideProfile = guideProfileRepository.findByUser_Id(userId).orElseThrow(GuideProfileNotFoundException::new);

        String oldImageUrl = guideProfile.getImageUrl();
        String newImageUrl = imageService.updateImage(oldImageUrl, imageFile);

        guideProfile.updateImageUrl(newImageUrl);

        return new GuideImageUpdateResponse(guideProfile.getImageUrl());
    }

    @Transactional
    public GuidePortfolioUpdateResponse updateMyPortfolios(Long userId, GuidePortfolioUpdateRequest request) {
        GuideProfile guideProfile = guideProfileRepository.findByUser_Id(userId).orElseThrow(GuideProfileNotFoundException::new);
        List<GuidePortfolio> portfolios = request.portfolios() == null ? List.of()
                : request.portfolios().stream()
                        .map(item -> item.toGuidePortfolio(guideProfile))
                        .toList();
        guideProfile.updatePortfolios(portfolios);

        List<PortfolioResponse> portfolioResponses = guideProfile.getPortfolios().stream()
                .map(p -> new PortfolioResponse(p.getType().name(), p.getUrl()))
                .toList();

        return new GuidePortfolioUpdateResponse(portfolioResponses);
    }
}