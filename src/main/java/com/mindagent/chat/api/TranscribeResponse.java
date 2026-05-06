package com.mindagent.chat.api;

public record TranscribeResponse(
        String transcript,
        String filename,
        String source
) {
}
