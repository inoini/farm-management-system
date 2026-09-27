package com.example.demo.security;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.service.RegistrationService;

@Controller
public class LoginController {

    private final RegistrationService registrationService;

    public LoginController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String displayName,
            @RequestParam String farmName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            Model model) {
        try {
            registrationService.register(displayName, farmName, username, email, password, passwordConfirm);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("displayName", clean(displayName));
            model.addAttribute("farmName", clean(farmName));
            model.addAttribute("username", clean(username));
            model.addAttribute("email", clean(email));
            return "register";
        }
    }

    @GetMapping("/join")
    public String join(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("inviteCode", clean(code));
        return "join";
    }

    @PostMapping("/join")
    public String joinFarm(
            @RequestParam String inviteCode,
            @RequestParam String displayName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            Model model) {
        try {
            registrationService.joinFarm(inviteCode, displayName, username, email, password, passwordConfirm);
            return "redirect:/login?joined";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("inviteCode", clean(inviteCode));
            model.addAttribute("displayName", clean(displayName));
            model.addAttribute("username", clean(username));
            model.addAttribute("email", clean(email));
            return "join";
        }
    }

    @GetMapping("/auth/csrf")
    @ResponseBody
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of(
                "parameterName", csrfToken.getParameterName(),
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken());
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
