package com.mindagent.chat.core;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class WhisperSpeechToTextService implements SpeechToTextService {

    @Value("${mindagent.whisper.enabled:false}")
    private boolean whisperEnabled;

    @Value("${mindagent.whisper.base-url:http://localhost:8000/v1}")
    private String whisperBaseUrl;

    @Value("${mindagent.whisper.model:whisper-1}")
    private String whisperModel;

    @Value("${mindagent.whisper.api-key:}")
    private String whisperApiKey;

    @Override
    public String transcribe(String base64AudioContent) {
        if (!whisperEnabled) {
            return "WHISPER_DISABLED_PLACEHOLDER";
        }

        try {
            String sanitized = stripDataUrlPrefix(base64AudioContent);
            byte[] audioBytes = Base64.getDecoder().decode(sanitized);
            return transcribe(audioBytes, "input.wav");
        } catch (Exception ex) {
            return "WHISPER_TRANSCRIBE_FAILED";
        }
    }

    @Override
    public String transcribe(byte[] audioBytes, String filename) {
        if (!whisperEnabled) {
            return "WHISPER_DISABLED_PLACEHOLDER";
        }

        try {
            String safeFilename = (filename == null || filename.isBlank()) ? "input.wav" : filename;
            Resource audioFile = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return safeFilename;
                }
            };

            ApiKey apiKey = (whisperApiKey == null || whisperApiKey.isBlank())
                    ? new NoopApiKey()
                    : () -> whisperApiKey;

            OpenAiAudioApi audioApi = OpenAiAudioApi.builder()
                    .baseUrl(whisperBaseUrl)
                    .apiKey(apiKey)
                    .build();

            OpenAiAudioTranscriptionOptions options = new OpenAiAudioTranscriptionOptions();
            options.setModel(whisperModel);

            OpenAiAudioTranscriptionModel model = new OpenAiAudioTranscriptionModel(audioApi, options);
            AudioTranscriptionResponse response = model.call(new AudioTranscriptionPrompt(audioFile, options));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return "WHISPER_EMPTY_TRANSCRIPT";
            }
            return response.getResult().getOutput();
        } catch (Exception ex) {
            return "WHISPER_TRANSCRIBE_FAILED";
        }
    }

    private String stripDataUrlPrefix(String content) {
        int commaIndex = content.indexOf(',');
        if (commaIndex > 0 && content.substring(0, commaIndex).contains("base64")) {
            return content.substring(commaIndex + 1);
        }
        return content;
    }
}
