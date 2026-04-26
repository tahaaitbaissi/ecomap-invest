package com.example.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.DynamicProfileResponse;
import com.example.backend.controllers.dto.GenerateDynamicProfileRequest;
import com.example.backend.controllers.dto.TagWeightDto;
import com.example.backend.services.profile.DynamicProfileService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
                Instant.now(),
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
        when(dynamicProfileService.listMine(eq("a@a.com"))).thenReturn(List.of());
        ResponseEntity<?> res = profileController.my(auth());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}

