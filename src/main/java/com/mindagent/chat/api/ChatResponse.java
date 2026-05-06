package com.mindagent.chat.api;

import com.mindagent.chat.core.ToolExecutionResult;

import java.util.Map;
import java.util.List;

public record ChatResponse(
        String sessionId,
        String reply,
        String emotionLabel,
        String routePolicy,
        Map<String, Double> emotionScores,
        List<String> normalizedInputs,
        List<ToolExecutionResult> toolExecutionResults
) {
}
