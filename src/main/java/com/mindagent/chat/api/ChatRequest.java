package com.mindagent.chat.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;

import java.util.List;

public record ChatRequest(
        String sessionId,
        @NotBlank(message = "userId is required") String userId,
        @NotBlank(message = "message is required") String message,
        List<@Valid ModalInput> modalInputs
) {
}
