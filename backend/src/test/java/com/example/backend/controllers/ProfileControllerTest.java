package com.example.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.DynamicProfileResponse;
import com.example.backend.controllers.dto.ProfileScoreNormDebugResponse;
import com.example.backend.controllers.dto.GenerateDynamicProfileRequest;
import com.example.backend.controllers.dto.TagWeightDto;
import com.example.backend.controllers.dto.UpdateDynamicProfileRequest;
import com.example.backend.controllers.dto.HexExplanationContextDto;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.services.ProfileScoreScaleService;
import com.example.backend.services.explain.HexExplanationContextBuilder;
import com.example.backend.services.profile.DynamicProfileService;
import com.example.backend.services.profile.ProfileTagCatalog;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private DynamicProfileService dynamicProfileService;

    @Mock
    private ProfileTagCatalog profileTagCatalog;

    @Mock
    private HexagonRawScoringSupport hexagonRawScoringSupport;

    @Mock
    private ProfileScoreScaleService profileScoreScaleService;

    @Mock
    private HexExplanationContextBuilder hexExplanationContextBuilder;

    @InjectMocks
    private ProfileController profileController;

    private static UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                "a@a.com", "x", List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
    }

    @Test
    void generate_returns201() {
        DynamicProfileResponse created = new DynamicProfileResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "q",
                "q",
                Instant.now(),
                Instant.now(),
                null,
                List.of(new TagWeightDto("amenity=school", 1.0)),
                List.of(new TagWeightDto("amenity=cafe", 1.0)));
        when(dynamicProfileService.generateForUserEmail(eq("a@a.com"), eq("q")))
                .thenReturn(created);

        ResponseEntity<?> res = profileController.generate(
                auth(),
                new GenerateDynamicProfileRequest("q"),
                UriComponentsBuilder.fromUriString("http://localhost"));

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
    }

    @Test
    void my_returns200() {
        Pageable pageable = PageRequest.of(0, 20);
        when(dynamicProfileService.listMine(eq("a@a.com"), any(Pageable.class), eq(false)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
        ResponseEntity<?> res = profileController.my(auth(), false, pageable);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void tags_returnsSupportedCatalog() {
        when(profileTagCatalog.options()).thenReturn(List.of());
        ResponseEntity<?> res = profileController.tags();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(profileTagCatalog).options();
    }

    @Test
    void update_returns200() {
        UUID id = UUID.randomUUID();
        DynamicProfileResponse updated = new DynamicProfileResponse(
                id,
                UUID.randomUUID(),
                "Updated",
                "q",
                Instant.now(),
                Instant.now(),
                null,
                List.of(new TagWeightDto("amenity=school", 1.0)),
                List.of(new TagWeightDto("amenity=cafe", 1.0)));
        UpdateDynamicProfileRequest req = new UpdateDynamicProfileRequest("Updated", null, null);
        when(dynamicProfileService.updateMine("a@a.com", id, req)).thenReturn(updated);

        ResponseEntity<?> res = profileController.update(auth(), id, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void duplicate_returns201() {
        UUID sourceId = UUID.randomUUID();
        DynamicProfileResponse created = new DynamicProfileResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Copy",
                "q",
                Instant.now(),
                Instant.now(),
                null,
                List.of(new TagWeightDto("amenity=school", 1.0)),
                List.of(new TagWeightDto("amenity=cafe", 1.0)));
        when(dynamicProfileService.duplicateMine("a@a.com", sourceId)).thenReturn(created);

        ResponseEntity<?> res = profileController.duplicate(
                auth(),
                sourceId,
                UriComponentsBuilder.fromUriString("http://localhost"));
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
    }

    @Test
    void archive_returns204() {
        UUID id = UUID.randomUUID();
        ResponseEntity<?> res = profileController.archive(auth(), id);
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
        verify(dynamicProfileService).archiveMine("a@a.com", id);
    }

    @Test
    void scoreNormDebug_returns200_andChecksOwnership() {
        UUID id = UUID.randomUUID();
        DynamicProfileResponse profile = new DynamicProfileResponse(
                id,
                UUID.randomUUID(),
                "p",
                "q",
                Instant.now(),
                Instant.now(),
                null,
                List.of(),
                List.of());
        when(dynamicProfileService.getMineById("a@a.com", id)).thenReturn(profile);
        HexScoringConfig cfg = new HexScoringConfig(List.of(), List.of(), false);
        when(hexagonRawScoringSupport.buildConfigForProfile(id)).thenReturn(cfg);
        ProfileScoreNormDebugResponse debug =
                new ProfileScoreNormDebugResponse(
                        id,
                        3,
                        25000,
                        0.0,
                        10.0,
                        0.5,
                        5.0,
                        9.5,
                        0.5,
                        9.5,
                        ProfileScoreNormDebugResponse.StretchMode.PERCENTILE,
                        List.of());
        when(profileScoreScaleService.computeNormDebug(id, cfg)).thenReturn(debug);

        ResponseEntity<ProfileScoreNormDebugResponse> res =
                profileController.scoreNormDebug(auth(), id);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(3, res.getBody().sampleSize());
        verify(dynamicProfileService).getMineById("a@a.com", id);
        verify(profileScoreScaleService).computeNormDebug(id, cfg);
    }

    @Test
    void hexDetails_returns200() {
        UUID id = UUID.randomUUID();
        DynamicProfileResponse profile = new DynamicProfileResponse(
                id,
                UUID.randomUUID(),
                "p",
                "q",
                Instant.now(),
                Instant.now(),
                null,
                List.of(),
                List.of());
        when(dynamicProfileService.getMineById("a@a.com", id)).thenReturn(profile);

        HexExplanationContextDto dto =
                new HexExplanationContextDto(
                        id,
                        "n",
                        "q",
                        "89283082803ffff",
                        9,
                        false,
                        9,
                        1,
                        "x",
                        0,
                        0,
                        10,
                        0,
                        20,
                        false,
                        50.0,
                        0.1,
                        5,
                        2,
                        List.of(),
                        List.of(),
                        new HexExplanationContextDto.DemographicsSnapshot(true, 20000.0, 0.1),
                        0L,
                        null,
                        null,
                        null);
        when(hexExplanationContextBuilder.build(id, "89283082803ffff", null)).thenReturn(dto);

        ResponseEntity<?> res = profileController.hexDetails(auth(), id, "89283082803ffff", null);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(dto, res.getBody());
    }

    @Test
    void hexDetails_returns400_whenBuilderThrowsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(dynamicProfileService.getMineById("a@a.com", id))
                .thenReturn(
                        new DynamicProfileResponse(
                                id,
                                UUID.randomUUID(),
                                "p",
                                "q",
                                Instant.now(),
                                Instant.now(),
                                null,
                                List.of(),
                                List.of()));
        when(hexExplanationContextBuilder.build(eq(id), any(), any()))
                .thenThrow(new IllegalArgumentException("off-grid"));

        ResponseEntity<?> res = profileController.hexDetails(auth(), id, "bad", null);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals("off-grid", res.getBody());
    }
}
