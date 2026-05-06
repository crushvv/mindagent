package com.mindagent.chat.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmotionFusionServiceTests {

    private final EmotionFusionService service = new EmotionFusionService();

    @Test
    void shouldReturnRiskWhenRiskKeywordsExist() {
        EmotionFusionService.EmotionFusionResult result = service.fuse(
                List.of("TEXT: 我有点不想活了")
        );

        assertEquals("risk", result.emotionLabel());
        assertEquals("RAG_AUGMENTED_GENERATION", result.routePolicy());
        assertTrue(result.emotionScores().get("risk") > 0.5);
    }

    @Test
    void shouldReturnChatForNeutralInput() {
        EmotionFusionService.EmotionFusionResult result = service.fuse(
                List.of("TEXT: 今天天气不错", "AUDIO_TRANSCRIPT: 我想聊聊今天学习了什么")
        );

        assertEquals("chat", result.emotionLabel());
        assertEquals("DIRECT_GENERATION", result.routePolicy());
    }
}
