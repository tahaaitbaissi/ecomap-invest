package com.example.backend.services.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LLMServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private LLMService llmService;

    @BeforeEach
    void setUp() {
        llmService = new LLMService(chatLanguageModel, new ObjectMapper());
    }

    @Test
    void generateProfileConfig_returnsParsedProfile_whenModelReturnsStrictJson() {
        String body =
                "{\"drivers\":[{\"tag\":\"amenity=school\",\"weight\":1.0}],\"competitors\":[{\"tag\":\"amenity=cafe\",\"weight\":0.9}]}";
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.from(body)));

        var opt = llmService.generateProfileConfig("coffee shop near schools");
        assertThat(opt).isPresent();
        assertThat(opt.get().drivers()).hasSize(1);
        assertThat(opt.get().competitors()).hasSize(1);
        assertThat(opt.get().drivers().get(0).tag()).isEqualTo("amenity=school");
    }

    @Test
    void generateProfileConfig_acceptsJsonInsideFence() {
        String body =
                "```json\n"
                        + "{\"drivers\":[{\"tag\":\"office=company\",\"weight\":1.1}],\"competitors\":[{\"tag\":\"amenity=restaurant\",\"weight\":1.0}]}\n"
                        + "```";
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(body)));

        var opt = llmService.generateProfileConfig("office zone");
        assertThat(opt).isPresent();
    }

    @Test
    void generateProfileConfig_throwsWhenTagMalformed() {
        String body =
                "{\"drivers\":[{\"tag\":\"INVALID TAG\",\"weight\":1.0}],\"competitors\":[{\"tag\":\"amenity=cafe\",\"weight\":0.9}]}";
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(body)));

        assertThatThrownBy(() -> llmService.generateProfileConfig("x")).isInstanceOf(IllegalArgumentException.class);
    }
}
