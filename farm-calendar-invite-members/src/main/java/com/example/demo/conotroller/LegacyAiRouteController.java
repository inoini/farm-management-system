package com.example.demo.conotroller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LegacyAiRouteController {

    @GetMapping("/ai")
    public String legacyAiGet() {
        return "redirect:/calendar";
    }

    @PostMapping("/ai")
    public String legacyAiPost() {
        return "redirect:/calendar";
    }
}
