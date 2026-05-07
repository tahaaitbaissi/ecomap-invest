package com.example.backend.services.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.TagWeightDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamicProfileGenerationServiceTest {

    @Mock
    private FallbackProfileProvider fallbackProfileProvider;

    @Mock
    private LLMService llmService;

    private DynamicProfileGenerationService generationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        generationService =
                new DynamicProfileGenerationService(fallbackProfileProvider, llmService, new ProfileTagCatalog());
    }

    @Test
    void usesLlmWhenPresent() {
        var fromLlm =
                new FallbackProfileProvider.ProfileConfig(
                        List.of(new TagWeightDto("amenity=school", 1.0)),
                        List.of(new TagWeightDto("amenity=restaurant", 1.0)));
        when(llmService.generateProfileConfig("hello")).thenReturn(Optional.of(fromLlm));

        var out = generationService.generate("hello");
        assertThat(out.drivers()).hasSize(1);
        assertThat(out.competitors()).hasSize(1);
    }

    @Test
    void fallsBackWhenLlmEmpty() {
        when(llmService.generateProfileConfig("hello")).thenReturn(Optional.empty());
        when(fallbackProfileProvider.findBestMatch("hello"))
                .thenReturn(
                        new FallbackProfileProvider.ProfileConfig(
                                List.of(new TagWeightDto("shop=convenience", 1.0)),
                                List.of(new TagWeightDto("amenity=cafe", 1.0))));

        var out = generationService.generate("hello");
        assertThat(out.drivers().get(0).tag()).isEqualTo("shop=supermarket");
    }

    @Test
    void filtersUnsupportedLlmTagsAndFallsBackWhenSectionEmpty() {
        when(llmService.generateProfileConfig("legal")).thenReturn(Optional.of(
                new FallbackProfileProvider.ProfileConfig(
                        List.of(new TagWeightDto("amenity=notary", 1.0)),
                        List.of(new TagWeightDto("unsupported=value", 1.0)))));
        when(fallbackProfileProvider.findBestMatch(""))
                .thenReturn(
                        new FallbackProfileProvider.ProfileConfig(
                                List.of(new TagWeightDto("amenity=school", 1.0)),
                                List.of(new TagWeightDto("amenity=cafe", 1.0))));

        var out = generationService.generate("legal");
        // Drivers should keep valid canonicalized tags (amenity=notary aliases to office=company in catalog)
        // while competitors falls back (unsupported tag was dropped).
        assertThat(out.drivers()).extracting(TagWeightDto::tag).containsExactly("office=company");
        assertThat(out.competitors()).extracting(TagWeightDto::tag).containsExactly("amenity=cafe");
    }
}
