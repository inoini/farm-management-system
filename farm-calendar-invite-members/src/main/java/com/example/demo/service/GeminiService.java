package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String ask(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI機能のAPIキーが設定されていません。Render の環境変数 GEMINI_API_KEY を設定すると利用できます。";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", question)))),
                "generationConfig", Map.of(
                        "temperature", 0.35,
                        "maxOutputTokens", 2048));

        try {
            Map<?, ?> response = restTemplate.postForObject(url, body, Map.class);
            if (response == null) {
                return "AIから回答を取得できませんでした。時間をおいて再度お試しください。";
            }

            Object candidatesValue = response.get("candidates");
            if (!(candidatesValue instanceof List<?> candidates) || candidates.isEmpty()) {
                return "AIから回答を取得できませんでした。質問内容を少し変えて再度お試しください。";
            }
            if (!(candidates.get(0) instanceof Map<?, ?> candidate)) {
                return "AIの回答形式を読み取れませんでした。";
            }
            if (!(candidate.get("content") instanceof Map<?, ?> content)) {
                return "AIの回答内容を読み取れませんでした。";
            }
            Object partsValue = content.get("parts");
            if (!(partsValue instanceof List<?> parts) || parts.isEmpty()
                    || !(parts.get(0) instanceof Map<?, ?> part)) {
                return "AIの回答本文を読み取れませんでした。";
            }
            Object text = part.get("text");
            return text == null ? "AIの回答本文が空でした。" : text.toString();

        } catch (Exception ex) {
            // APIキーを含むURLが例外メッセージに入る可能性があるため、詳細メッセージは画面にもログにも出さない。
            log.warn("Gemini API request failed: {}", ex.getClass().getSimpleName());
            return "AI通信でエラーが発生しました。API設定または通信状態を確認して、もう一度お試しください。";
        }
    }
}
