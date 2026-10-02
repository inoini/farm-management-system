package com.example.demo.conotroller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.repository.UserAccountRepository;

@Controller
public class DashboardController {

    private final UserAccountRepository userAccountRepository;

    public DashboardController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        // Public visitors see the service introduction page.
        // Any signed-in user who reaches "/" from an in-app "back to top" link
        // is always returned to the work calendar.
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/calendar";
        }

        model.addAttribute("registeredUserCount", userAccountRepository.count());
        return "index";
    }

    // Keep old bookmarks working after retiring the dashboard.
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/calendar";
    }
}
