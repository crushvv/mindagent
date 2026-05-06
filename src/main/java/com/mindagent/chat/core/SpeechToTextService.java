package com.mindagent.chat.core;

public interface SpeechToTextService {
    String transcribe(String base64AudioContent);

    String transcribe(byte[] audioBytes, String filename);
}
