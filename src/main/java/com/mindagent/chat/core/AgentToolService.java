package com.mindagent.chat.core;

import java.util.List;

public interface AgentToolService {
    List<ToolExecutionResult> executeByEmotion(
            String sessionId,
            String userId,
            String emotionLabel,
            String routePolicy,
            String reply
    );
}
