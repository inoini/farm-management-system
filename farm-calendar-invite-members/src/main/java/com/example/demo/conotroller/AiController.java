package com.example.demo.conotroller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.FarmAiContextService;
import com.example.demo.service.GeminiService;

@Controller
public class AiController {

    private final GeminiService geminiService;
    private final FarmAiContextService farmAiContextService;

    public AiController(GeminiService geminiService, FarmAiContextService farmAiContextService) {
        this.geminiService = geminiService;
        this.farmAiContextService = farmAiContextService;
    }

    @GetMapping("/ai")
    public String ai(Model model) {
        addDashboard(model);
        return "ai";
    }

    @PostMapping("/ai")
    public String ask(@RequestParam String question, Model model) {
        String normalizedQuestion = question == null ? "" : question.strip();
        if (normalizedQuestion.isBlank()) {
            model.addAttribute("aiError", "相談内容を入力してください。");
            addDashboard(model);
            return "ai";
        }
        if (normalizedQuestion.length() > 2000) {
            normalizedQuestion = normalizedQuestion.substring(0, 2000);
        }

        String prompt = farmAiContextService.buildPrompt(normalizedQuestion);
        model.addAttribute("question", normalizedQuestion);
        model.addAttribute("answer", geminiService.ask(prompt));
        addDashboard(model);
        return "ai";
    }

    private void addDashboard(Model model) {
        for (Map.Entry<String, Object> entry : farmAiContextService.dashboard().entrySet()) {
            model.addAttribute(entry.getKey(), entry.getValue());
        }
    }
}
