package com.example.backend.services.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.TagWeightDto;
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
import org.mockito.InjectMocks;
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
                profileService);

        User u = new User();
        u.setId(7L);
        u.setEmail("a@a.com");
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        var cfg = new FallbackProfileProvider.ProfileConfig(
                List.of(new TagWeightDto("amenity=school", 1.0)),
                List.of(new TagWeightDto("amenity=cafe", 1.0)));
        when(generationService.generate("hello")).thenReturn(cfg);

        when(dynamicProfileRepository.save(any(DynamicProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var res = dynamicProfileService.generateForUserEmail("a@a.com", "hello");
        assertEquals(7L, res.userId());
        assertEquals("hello", res.userQuery());

        verify(profileService).notifyProfileGeneratedAfterPersist(any(UUID.class), eq(7L), eq("hello"));
    }

    @Test
    void getMineById_forbiddenWhenNotOwner() {
        DynamicProfileService dynamicProfileService = new DynamicProfileService(
                dynamicProfileRepository,
                userService,
                generationService,
                new ObjectMapper(),
                profileService);

        User u = new User();
        u.setId(1L);
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        DynamicProfile p = new DynamicProfile();
        p.setId(UUID.randomUUID());
        p.setUserId(2L);
        p.setUserQuery("q");
        p.setGeneratedAt(Instant.now());

        when(dynamicProfileRepository.findById(p.getId())).thenReturn(Optional.of(p));

        assertThrows(
                AccessDeniedException.class,
                () -> dynamicProfileService.getMineById("a@a.com", p.getId()));
    }
}

