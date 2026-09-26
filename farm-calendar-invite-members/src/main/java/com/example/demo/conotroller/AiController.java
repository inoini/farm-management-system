package com.example.demo.conotroller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Crop;
import com.example.demo.repository.CropRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.GeminiService;

@Controller
public class AiController {

    private final GeminiService geminiService;
    private final CropRepository cropRepository;
    private final CurrentUserService currentUser;

    public AiController(GeminiService geminiService, CropRepository cropRepository, CurrentUserService currentUser) {
        this.geminiService = geminiService;
        this.cropRepository = cropRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/ai")
    public String ai() { return "ai"; }

    @PostMapping("/ai")
    public String ask(@RequestParam String question, Model model) {
        List<Crop> crops = cropRepository.findAllByOwnerEmailOrderByIdDesc(currentUser.email());
        StringBuilder cropInfo = new StringBuilder();
        for (Crop crop : crops) {
            cropInfo.append("作物：").append(crop.getCropName()).append("\n")
                    .append("品種：").append(crop.getVariety()).append("\n")
                    .append("圃場：").append(crop.getFieldName()).append("\n")
                    .append("植付日：").append(crop.getPlantingDate()).append("\n")
                    .append("収穫予定：").append(crop.getHarvestDate()).append("\n\n");
        }
        String prompt = "あなたは農業専門AIアシスタントです。\n以下は現在管理している作物情報です。\n\n"
                + cropInfo + "\n農家からの質問：\n" + question
                + "\n\n日本語で分かりやすく回答してください。可能なら原因と対策を説明してください。";
        model.addAttribute("question", question);
        model.addAttribute("answer", geminiService.ask(prompt));
        return "ai";
    }
}
