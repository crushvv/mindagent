package com.mindagent.chat.core;

import com.mindagent.chat.api.ChatRequest;
import com.mindagent.chat.api.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final MultimodalInputService multimodalInputService;
    private final EmotionFusionService emotionFusionService;
    private final AgentToolService agentToolService;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            MultimodalInputService multimodalInputService,
            EmotionFusionService emotionFusionService,
            AgentToolService agentToolService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.multimodalInputService = multimodalInputService;
        this.emotionFusionService = emotionFusionService;
        this.agentToolService = agentToolService;
    }

    public ChatResponse chat(ChatRequest request) {
        //多模态输入标准化转为文本
        List<String> normalizedInputs = multimodalInputService.normalizeInputs(
                request.message(),
                request.modalInputs()
        );
        //拼接输入交给大模型
        String mergedInput = String.join("\n", normalizedInputs);
        //情绪融合
        EmotionFusionService.EmotionFusionResult fusion = emotionFusionService.fuse(normalizedInputs);
        //Sysyem prompt
        String prompt = """
                你是一个心理支持型助手，不提供医疗诊断。
                请用温和、共情、简洁的语气回应用户。
                当前用户情绪标签：%s
                用户多模态输入（已文本化）：
                %s
                """.formatted(fusion.emotionLabel(), mergedInput);

        String reply;
        try {
            reply = chatClient.prompt(prompt).call().content();//调用大模型
        } catch (Exception ex) {
            reply = "我在这里陪你。当前服务有些繁忙，我们可以先从你现在最困扰的一件事开始。";//大模型无响应返回
        }
        String resolvedSessionId = request.sessionId() == null ? "session-temp" : request.sessionId();//
        List<ToolExecutionResult> toolResults = agentToolService.executeByEmotion(
                resolvedSessionId,
                request.userId(),
                fusion.emotionLabel(),
                fusion.routePolicy(),
                reply
        );

        return new ChatResponse(
                resolvedSessionId,
                reply,
                fusion.emotionLabel(),
                fusion.routePolicy(),
                fusion.emotionScores(),
                normalizedInputs,
                toolResults
        );
    }
}
