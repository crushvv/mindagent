package com.mindagent.chat.core;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmotionFusionService {

    private static final String CHAT = "chat";
    private static final String SAD = "失落";
    private static final String ANXIOUS = "焦虑";
    private static final String RISK = "risk";

    private static final Map<String, Double> MODALITY_WEIGHTS = Map.of(
            "TEXT", 0.40,
            "AUDIO_TRANSCRIPT", 0.30,
            "IMAGE_DESCRIPTION_PLACEHOLDER", 0.20,
            "VIDEO_AUDIO_TRANSCRIPT", 0.10
    );

    public EmotionFusionResult fuse(List<String> normalizedInputs) {
        Map<String, Double> scores = new HashMap<>();
        scores.put(CHAT, 0.0);
        scores.put(SAD, 0.0);
        scores.put(ANXIOUS, 0.0);
        scores.put(RISK, 0.0);

        if (normalizedInputs == null || normalizedInputs.isEmpty()) {
            scores.put(CHAT, 1.0);
            return result(scores);
        }

        for (String input : normalizedInputs) {
            if (input == null || input.isBlank()) {
                continue;
            }
            String modality = extractModality(input);
            double weight = MODALITY_WEIGHTS.getOrDefault(modality, 0.25);
            Map<String, Double> single = scoreSingleInput(input);
            for (Map.Entry<String, Double> entry : single.entrySet()) {
                scores.put(entry.getKey(), scores.get(entry.getKey()) + entry.getValue() * weight);
            }
        }

        normalize(scores);
        return result(scores);
    }

    private EmotionFusionResult result(Map<String, Double> scores) {
        String label = scores.entrySet()
                .stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(CHAT);
        String routePolicy = CHAT.equals(label) ? "DIRECT_GENERATION" : "RAG_AUGMENTED_GENERATION";
        return new EmotionFusionResult(label, routePolicy, scores);
    }

    private Map<String, Double> scoreSingleInput(String input) {
        String lower = input.toLowerCase(Locale.ROOT);

        List<String> riskKeywords = List.of("自杀", "不想活", "结束生命", "伤害自己", "kill myself");
        if (containsAny(lower, riskKeywords)) {
            return Map.of(CHAT, 0.0, SAD, 0.1, ANXIOUS, 0.1, RISK, 0.8);
        }

        List<String> anxiousKeywords = List.of("焦虑", "紧张", "睡不着", "担心", "害怕", "恐慌");
        if (containsAny(lower, anxiousKeywords)) {
            return Map.of(CHAT, 0.1, SAD, 0.1, ANXIOUS, 0.7, RISK, 0.1);
        }

        List<String> sadKeywords = List.of("失落", "低落", "难过", "无助", "没意义", "孤独");
        if (containsAny(lower, sadKeywords)) {
            return Map.of(CHAT, 0.1, SAD, 0.7, ANXIOUS, 0.1, RISK, 0.1);
        }

        return Map.of(CHAT, 0.8, SAD, 0.1, ANXIOUS, 0.1, RISK, 0.0);
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractModality(String input) {
        int idx = input.indexOf(':');
        if (idx <= 0) {
            return "TEXT";
        }
        return input.substring(0, idx).trim();
    }

    private void normalize(Map<String, Double> scores) {
        double total = new ArrayList<>(scores.values()).stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            scores.put(CHAT, 1.0);
            scores.put(SAD, 0.0);
            scores.put(ANXIOUS, 0.0);
            scores.put(RISK, 0.0);
            return;
        }
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            scores.put(entry.getKey(), round(entry.getValue() / total));
        }
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public record EmotionFusionResult(String emotionLabel, String routePolicy, Map<String, Double> emotionScores) {
    }
}
