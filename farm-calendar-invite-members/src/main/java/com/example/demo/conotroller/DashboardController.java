package com.example.demo.conotroller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        // Public visitors see the service introduction page.
        // Any signed-in user who reaches "/" from an in-app "back to top" link
        // is always returned to the work calendar.
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/calendar";
        }
        return "index";
    }

    // Keep old bookmarks working after retiring the dashboard.
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/calendar";
    }
}
