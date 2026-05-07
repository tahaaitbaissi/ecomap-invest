package com.example.backend.services.profile;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.DynamicProfileResponse;
import com.example.backend.controllers.dto.TagWeightDto;
import com.example.backend.controllers.dto.UpdateDynamicProfileRequest;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.entities.User;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.services.ProfileService;
import com.example.backend.services.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
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
    private final ProfileTagCatalog profileTagCatalog;

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
        row.setName(defaultName(query));
        Instant now = Instant.now();
        row.setGeneratedAt(now);
        row.setUpdatedAt(now);
        row.setDriversConfig(driversJson);
        row.setCompetitorsConfig(competitorsJson);

        DynamicProfile saved = dynamicProfileRepository.save(row);

        // Publish AFTER_COMMIT event for cache invalidation
        profileService.notifyProfileGeneratedAfterPersist(saved.getId(), saved.getUserId(), saved.getUserQuery());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<DynamicProfileResponse> listMine(String userEmail, Pageable pageable) {
        return listMine(userEmail, pageable, false);
    }

    @Transactional(readOnly = true)
    public Page<DynamicProfileResponse> listMine(String userEmail, Pageable pageable, boolean includeArchived) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must be provided");
        }
        User user = userService.getUserByEmail(userEmail);
        Page<DynamicProfile> profiles = includeArchived
                ? dynamicProfileRepository.findByUserId(user.getId(), pageable)
                : dynamicProfileRepository.findByUserIdAndArchivedAtIsNull(user.getId(), pageable);
        return profiles.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DynamicProfileResponse getMineById(String userEmail, UUID profileId) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must be provided");
        }
        if (profileId == null) {
            throw new IllegalArgumentException("profileId must be provided");
        }
        return toResponse(getOwnedActiveEntity(userEmail, profileId));
    }

    @Transactional(readOnly = true)
    public DynamicProfile getOwnedActiveEntity(String userEmail, UUID profileId) {
        DynamicProfile p = getOwnedEntity(userEmail, profileId);
        if (p.getArchivedAt() != null) {
            throw new NoSuchElementException("Profile not found");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public DynamicProfile getOwnedEntity(String userEmail, UUID profileId) {
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
        return p;
    }

    @Transactional
    public DynamicProfileResponse updateMine(String userEmail, UUID profileId, UpdateDynamicProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must be provided");
        }
        DynamicProfile p = getOwnedActiveEntity(userEmail, profileId);
        boolean changed = false;
        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            p.setName(name.length() > 120 ? name.substring(0, 120) : name);
            changed = true;
        }
        if (request.drivers() != null) {
            List<TagWeightDto> drivers = cleanTags(request.drivers(), "drivers");
            if (drivers.isEmpty()) {
                throw new IllegalArgumentException("drivers must not be empty");
            }
            // remove overlaps if competitors already present (or will be updated later)
            List<TagWeightDto> currentCompetitors =
                    request.competitors() != null ? cleanTags(request.competitors(), "competitors") : cleanTags(toTagWeightList(p.getCompetitorsConfig()), "competitors");
            drivers = removeOverlaps(drivers, currentCompetitors, "drivers");
            p.setDriversConfig(objectMapper.valueToTree(drivers));
            changed = true;
        }
        if (request.competitors() != null) {
            List<TagWeightDto> competitors = cleanTags(request.competitors(), "competitors");
            if (competitors.isEmpty()) {
                throw new IllegalArgumentException("competitors must not be empty");
            }
            // If drivers were not updated in this request, still ensure no overlaps against existing drivers.
            List<TagWeightDto> currentDrivers =
                    request.drivers() != null ? cleanTags(request.drivers(), "drivers") : cleanTags(toTagWeightList(p.getDriversConfig()), "drivers");
            competitors = removeOverlaps(competitors, currentDrivers, "competitors");
            p.setCompetitorsConfig(objectMapper.valueToTree(competitors));
            changed = true;
        }
        if (changed) {
            p.setUpdatedAt(Instant.now());
        }
        DynamicProfile saved = dynamicProfileRepository.save(p);
        profileService.notifyProfileGeneratedAfterPersist(saved.getId(), saved.getUserId(), saved.getUserQuery());
        return toResponse(saved);
    }

    @Transactional
    public DynamicProfileResponse duplicateMine(String userEmail, UUID profileId) {
        DynamicProfile p = getOwnedActiveEntity(userEmail, profileId);
        Instant now = Instant.now();
        DynamicProfile copy = new DynamicProfile();
        copy.setId(UUID.randomUUID());
        copy.setUserId(p.getUserId());
        copy.setName(defaultName(p.getName() + " copy"));
        copy.setUserQuery(p.getUserQuery());
        copy.setDriversConfig(p.getDriversConfig());
        copy.setCompetitorsConfig(p.getCompetitorsConfig());
        copy.setGeneratedAt(now);
        copy.setUpdatedAt(now);
        DynamicProfile saved = dynamicProfileRepository.save(copy);
        profileService.notifyProfileGeneratedAfterPersist(saved.getId(), saved.getUserId(), saved.getUserQuery());
        return toResponse(saved);
    }

    @Transactional
    public void archiveMine(String userEmail, UUID profileId) {
        DynamicProfile p = getOwnedActiveEntity(userEmail, profileId);
        Instant now = Instant.now();
        p.setArchivedAt(now);
        p.setUpdatedAt(now);
        dynamicProfileRepository.save(p);
        profileService.notifyProfileGeneratedAfterPersist(p.getId(), p.getUserId(), p.getUserQuery());
    }

    private DynamicProfileResponse toResponse(DynamicProfile p) {
        List<TagWeightDto> drivers = profileTagCatalog.canonicalize(toTagWeightList(p.getDriversConfig()), "profile-response.drivers");
        List<TagWeightDto> competitors =
                profileTagCatalog.canonicalize(toTagWeightList(p.getCompetitorsConfig()), "profile-response.competitors");
        return new DynamicProfileResponse(
                p.getId(),
                p.getUserId(),
                p.getName(),
                p.getUserQuery(),
                p.getGeneratedAt(),
                p.getUpdatedAt(),
                p.getArchivedAt(),
                drivers,
                competitors);
    }

    private static String defaultName(String query) {
        String trimmed = query == null ? "Commercial profile" : query.trim();
        if (trimmed.isBlank()) {
            trimmed = "Commercial profile";
        }
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }

    private List<TagWeightDto> cleanTags(List<TagWeightDto> tags, String fieldName) {
        if (tags == null) {
            return List.of();
        }
        List<TagWeightDto> out = tags.stream()
                .filter(t -> t != null && t.tag() != null && !t.tag().trim().isEmpty())
                .map(t -> new TagWeightDto(t.tag().trim(), normalizeWeight(t.weight())))
                .toList();
        out = profileTagCatalog.canonicalize(out, "profile-update." + fieldName);
        if (out.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must contain at least one tag");
        }
        return out;
    }

    private static List<TagWeightDto> removeOverlaps(List<TagWeightDto> primary, List<TagWeightDto> other, String fieldName) {
        Set<String> otherTags = new HashSet<>();
        for (TagWeightDto t : other) {
            if (t != null && t.tag() != null && !t.tag().isBlank()) {
                otherTags.add(t.tag());
            }
        }
        List<TagWeightDto> filtered =
                primary.stream()
                        .filter(t -> t != null && t.tag() != null && !otherTags.contains(t.tag()))
                        .toList();
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not overlap entirely with the other list");
        }
        return filtered;
    }

    private static double normalizeWeight(Double weight) {
        if (weight == null || !Double.isFinite(weight)) {
            return 1.0;
        }
        return Math.max(
                DynamicProfileGenerationService.MIN_WEIGHT,
                Math.min(DynamicProfileGenerationService.MAX_WEIGHT, weight));
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

