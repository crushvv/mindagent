package com.mindagent.chat.core;

public record ToolExecutionResult(
        String toolName,
        boolean success,
        String message
) {
}
