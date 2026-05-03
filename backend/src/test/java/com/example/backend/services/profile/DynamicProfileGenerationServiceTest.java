package com.example.backend.services.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.TagWeightDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamicProfileGenerationServiceTest {

    @Mock
    private FallbackProfileProvider fallbackProfileProvider;

    @Mock
    private LLMService llmService;

    @InjectMocks
    private DynamicProfileGenerationService generationService;

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
                                List.of(new TagWeightDto("shop=mall", 1.0)),
                                List.of(new TagWeightDto("amenity=cafe", 1.0))));

        var out = generationService.generate("hello");
        assertThat(out.drivers().get(0).tag()).isEqualTo("shop=mall");
    }
}
