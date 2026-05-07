package com.example.backend.services.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.TagWeightDto;
import com.example.backend.controllers.dto.UpdateDynamicProfileRequest;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.entities.User;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.services.ProfileService;
import com.example.backend.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DynamicProfileServiceTest {

    @Mock
    private DynamicProfileRepository dynamicProfileRepository;

    @Mock
    private UserService userService;

    @Mock
    private DynamicProfileGenerationService generationService;

    @Mock
    private ProfileService profileService;

    @Test
    void generate_persistsAndPublishesEvent() {
        DynamicProfileService dynamicProfileService = new DynamicProfileService(
                dynamicProfileRepository,
                userService,
                generationService,
                new ObjectMapper(),
                profileService,
                new ProfileTagCatalog());

        User u = new User();
        UUID userId = UUID.randomUUID();
        u.setId(userId);
        u.setEmail("a@a.com");
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        var cfg = new FallbackProfileProvider.ProfileConfig(
                List.of(new TagWeightDto("amenity=school", 1.0)),
                List.of(new TagWeightDto("amenity=cafe", 1.0)));
        when(generationService.generate("hello")).thenReturn(cfg);

        when(dynamicProfileRepository.save(any(DynamicProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var res = dynamicProfileService.generateForUserEmail("a@a.com", "hello");
        assertEquals(userId, res.userId());
        assertEquals("hello", res.userQuery());

        verify(profileService).notifyProfileGeneratedAfterPersist(any(UUID.class), eq(userId), eq("hello"));
    }

    @Test
    void getMineById_forbiddenWhenNotOwner() {
        DynamicProfileService dynamicProfileService = new DynamicProfileService(
                dynamicProfileRepository,
                userService,
                generationService,
                new ObjectMapper(),
                profileService,
                new ProfileTagCatalog());

        User u = new User();
        u.setId(UUID.randomUUID());
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        DynamicProfile p = new DynamicProfile();
        p.setId(UUID.randomUUID());
        p.setUserId(UUID.randomUUID());
        p.setName("q");
        p.setUserQuery("q");
        p.setGeneratedAt(Instant.now());
        p.setUpdatedAt(Instant.now());

        when(dynamicProfileRepository.findById(p.getId())).thenReturn(Optional.of(p));

        assertThrows(
                AccessDeniedException.class,
                () -> dynamicProfileService.getMineById("a@a.com", p.getId()));
    }

    @Test
    void updateMine_renamesAndEditsTags() {
        DynamicProfileService dynamicProfileService = newService();
        User u = new User();
        UUID userId = UUID.randomUUID();
        u.setId(userId);
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        DynamicProfile p = profile(userId);
        when(dynamicProfileRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(dynamicProfileRepository.save(any(DynamicProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var out = dynamicProfileService.updateMine(
                "a@a.com",
                p.getId(),
                new UpdateDynamicProfileRequest(
                        "New name",
                        List.of(new TagWeightDto("amenity=school", 2.0)),
                        List.of(new TagWeightDto("amenity=cafe", 0.5))));

        assertEquals("New name", out.name());
        assertEquals(1.5, out.drivers().get(0).weight());
        assertEquals(0.5, out.competitors().get(0).weight());
        verify(profileService).notifyProfileGeneratedAfterPersist(eq(p.getId()), eq(userId), eq("q"));
    }

    @Test
    void getMineById_canonicalizesStoredAliasesInResponse() {
        DynamicProfileService dynamicProfileService = newService();
        User u = new User();
        UUID userId = UUID.randomUUID();
        u.setId(userId);
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        ObjectMapper objectMapper = new ObjectMapper();
        DynamicProfile p = profile(userId);
        p.setDriversConfig(objectMapper.valueToTree(List.of(new TagWeightDto("amenity=lawyer", 1.2))));
        p.setCompetitorsConfig(objectMapper.valueToTree(List.of(new TagWeightDto("shop=financial_services", 0.7))));
        when(dynamicProfileRepository.findById(p.getId())).thenReturn(Optional.of(p));

        var out = dynamicProfileService.getMineById("a@a.com", p.getId());

        assertEquals("office=company", out.drivers().get(0).tag());
        assertEquals("amenity=bank", out.competitors().get(0).tag());
    }


    @Test
    void duplicateMine_copiesConfigWithNewId() {
        DynamicProfileService dynamicProfileService = newService();
        User u = new User();
        UUID userId = UUID.randomUUID();
        u.setId(userId);
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        DynamicProfile p = profile(userId);
        when(dynamicProfileRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(dynamicProfileRepository.save(any(DynamicProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var out = dynamicProfileService.duplicateMine("a@a.com", p.getId());
        assertNotNull(out.id());
        assertEquals(userId, out.userId());
        assertEquals("q copy", out.name());
        verify(profileService).notifyProfileGeneratedAfterPersist(any(UUID.class), eq(userId), eq("q"));
    }

    @Test
    void archiveMine_setsArchivedAt() {
        DynamicProfileService dynamicProfileService = newService();
        User u = new User();
        UUID userId = UUID.randomUUID();
        u.setId(userId);
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        DynamicProfile p = profile(userId);
        when(dynamicProfileRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(dynamicProfileRepository.save(any(DynamicProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        dynamicProfileService.archiveMine("a@a.com", p.getId());
        assertNotNull(p.getArchivedAt());
        verify(profileService).notifyProfileGeneratedAfterPersist(eq(p.getId()), eq(userId), eq("q"));
    }

    private DynamicProfileService newService() {
        return new DynamicProfileService(
                dynamicProfileRepository,
                userService,
                generationService,
                new ObjectMapper(),
                profileService,
                new ProfileTagCatalog());
    }

    private DynamicProfile profile(UUID userId) {
        ObjectMapper objectMapper = new ObjectMapper();
        DynamicProfile p = new DynamicProfile();
        p.setId(UUID.randomUUID());
        p.setUserId(userId);
        p.setName("q");
        p.setUserQuery("q");
        p.setGeneratedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        p.setDriversConfig(objectMapper.valueToTree(List.of(new TagWeightDto("amenity=school", 1.0))));
        p.setCompetitorsConfig(objectMapper.valueToTree(List.of(new TagWeightDto("amenity=cafe", 1.0))));
        return p;
    }
}

