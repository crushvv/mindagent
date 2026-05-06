package com.mindagent.chat.core;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockAgentToolServiceTests {

    @Test
    void shouldTriggerBothToolsForRisk() {
        MockAgentToolService service = new MockAgentToolService();
        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "excelEnabled", true);
        ReflectionTestUtils.setField(service, "excelFilePath", "target/test-consultation-risk.xlsx");

        List<ToolExecutionResult> results = service.executeByEmotion(
                "s1", "u1", "risk", "RAG_AUGMENTED_GENERATION", "reply"
        );

        assertEquals(2, results.size());
        assertEquals("sendRiskEmailTool", results.get(0).toolName());
        assertEquals("appendConsultationExcelTool", results.get(1).toolName());
    }

    @Test
    void shouldTriggerNoToolsForChat() {
        MockAgentToolService service = new MockAgentToolService();
        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "excelEnabled", true);
        ReflectionTestUtils.setField(service, "excelFilePath", "target/test-consultation-chat.xlsx");

        List<ToolExecutionResult> results = service.executeByEmotion(
                "s1", "u1", "chat", "DIRECT_GENERATION", "reply"
        );

        assertEquals(0, results.size());
    }
}
