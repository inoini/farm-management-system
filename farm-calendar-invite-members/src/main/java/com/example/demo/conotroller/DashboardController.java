package com.example.demo.conotroller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    // Keep old bookmarks working after retiring the dashboard.
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/";
    }
}
