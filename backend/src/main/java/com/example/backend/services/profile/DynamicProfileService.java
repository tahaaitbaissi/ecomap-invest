package com.example.backend.services.profile;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.DynamicProfileResponse;
import com.example.backend.controllers.dto.TagWeightDto;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.entities.User;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.services.ProfileService;
import com.example.backend.services.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DynamicProfileService {

    private static final TypeReference<List<TagWeightDto>> TAG_WEIGHT_LIST =
            new TypeReference<>() {};

    private final DynamicProfileRepository dynamicProfileRepository;
    private final UserService userService;
    private final DynamicProfileGenerationService generationService;
    private final ObjectMapper objectMapper;
    private final ProfileService profileService;

    @Transactional
    @Audited(action = "GENERATE_PROFILE")
    public DynamicProfileResponse generateForUserEmail(String userEmail, String rawQuery) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must be provided");
        }
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException("query must be provided");
        }

        String query = rawQuery.trim();
        User user = userService.getUserByEmail(userEmail);

        var cfg = generationService.generate(query);
        JsonNode driversJson = objectMapper.valueToTree(cfg.drivers());
        JsonNode competitorsJson = objectMapper.valueToTree(cfg.competitors());

        DynamicProfile row = new DynamicProfile();
        UUID id = UUID.randomUUID();
        row.setId(id);
        row.setUserId(user.getId());
        row.setUserQuery(query);
        row.setGeneratedAt(Instant.now());
        row.setDriversConfig(driversJson);
        row.setCompetitorsConfig(competitorsJson);

        DynamicProfile saved = dynamicProfileRepository.save(row);

        // Publish AFTER_COMMIT event for cache invalidation
        profileService.notifyProfileGeneratedAfterPersist(saved.getId(), saved.getUserId(), saved.getUserQuery());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<DynamicProfileResponse> listMine(String userEmail, Pageable pageable) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must be provided");
        }
        User user = userService.getUserByEmail(userEmail);
        return dynamicProfileRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DynamicProfileResponse getMineById(String userEmail, UUID profileId) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must be provided");
        }
        if (profileId == null) {
            throw new IllegalArgumentException("profileId must be provided");
        }
        User user = userService.getUserByEmail(userEmail);
        DynamicProfile p = dynamicProfileRepository.findById(profileId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));
        if (p.getUserId() == null || !p.getUserId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this profile");
        }
        return toResponse(p);
    }

    private DynamicProfileResponse toResponse(DynamicProfile p) {
        List<TagWeightDto> drivers = toTagWeightList(p.getDriversConfig());
        List<TagWeightDto> competitors = toTagWeightList(p.getCompetitorsConfig());
        return new DynamicProfileResponse(
                p.getId(),
                p.getUserId(),
                p.getUserQuery(),
                p.getGeneratedAt(),
                drivers,
                competitors);
    }

    private List<TagWeightDto> toTagWeightList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        try {
            List<TagWeightDto> v = objectMapper.convertValue(node, TAG_WEIGHT_LIST);
            return v == null ? List.of() : v;
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }
}

