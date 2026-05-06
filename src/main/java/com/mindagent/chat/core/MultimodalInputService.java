package com.mindagent.chat.core;

import com.mindagent.chat.api.ModalInput;
import com.mindagent.chat.api.ModalityType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MultimodalInputService {

    private final SpeechToTextService speechToTextService;

    public MultimodalInputService(SpeechToTextService speechToTextService) {
        this.speechToTextService = speechToTextService;
    }

    public List<String> normalizeInputs(String textMessage, List<ModalInput> modalInputs) {
        List<String> normalized = new ArrayList<>();

        if (textMessage != null && !textMessage.isBlank()) {
            normalized.add("TEXT: " + textMessage);
        }

        if (modalInputs == null || modalInputs.isEmpty()) {
            return normalized;
        }

        for (ModalInput input : modalInputs) {
            if (input == null || input.content() == null || input.content().isBlank()) {
                continue;
            }
            normalized.add(convertToTextEvidence(input));
        }

        return normalized;
    }

    private String convertToTextEvidence(ModalInput input) {
        ModalityType type = input.type();
        String content = input.content();

        if (type == null) {
            return "UNKNOWN: " + content;
        }

        return switch (type) {
            case TEXT -> "TEXT: " + content;
            case AUDIO -> "AUDIO_TRANSCRIPT: " + speechToTextService.transcribe(content);
            // TODO: replace placeholder with image understanding model output.
            case IMAGE -> "IMAGE_DESCRIPTION_PLACEHOLDER: " + content;
            // Phase 1 simplification: assume video content carries extracted audio track in base64.
            case VIDEO -> "VIDEO_AUDIO_TRANSCRIPT: " + speechToTextService.transcribe(content);
        };
    }
}
