package com.mindagent.chat.core;

import com.mindagent.chat.api.ModalInput;
import com.mindagent.chat.api.ModalityType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultimodalInputServiceTests {

    private final MultimodalInputService service = new MultimodalInputService(new SpeechToTextService() {
        @Override
        public String transcribe(String base64AudioContent) {
            return "transcribed_text";
        }

        @Override
        public String transcribe(byte[] audioBytes, String filename) {
            return "transcribed_text";
        }
    });

    @Test
    void shouldKeepTextAndConvertModalInputsToTextEvidence() {
        List<ModalInput> modalInputs = List.of(
                new ModalInput(ModalityType.AUDIO, "audio_base64_or_url"),
                new ModalInput(ModalityType.IMAGE, "image_base64_or_url"),
                new ModalInput(ModalityType.VIDEO, "video_base64_or_url")
        );

        List<String> normalized = service.normalizeInputs("plain text", modalInputs);

        assertEquals(4, normalized.size());
        assertEquals("TEXT: plain text", normalized.get(0));
        assertTrue(normalized.get(1).startsWith("AUDIO_TRANSCRIPT: "));
        assertTrue(normalized.get(2).startsWith("IMAGE_DESCRIPTION_PLACEHOLDER: "));
        assertTrue(normalized.get(3).startsWith("VIDEO_AUDIO_TRANSCRIPT: "));
    }
}
