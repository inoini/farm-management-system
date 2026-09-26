package com.example.demo.security;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.AccountRecoveryService;

@Controller
public class AccountRecoveryController {

    private final AccountRecoveryService recoveryService;

    public AccountRecoveryController(AccountRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @GetMapping("/forgot-username")
    public String forgotUsername() {
        return "forgot-username";
    }

    @PostMapping("/forgot-username")
    public String sendUsername(@RequestParam String email, Model model) {
        try {
            recoveryService.requestUsernameReminder(email);
            model.addAttribute("successMessage",
                    "入力したメールアドレスが登録されている場合、ユーザー名を送信しました。");
        } catch (IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        model.addAttribute("email", email == null ? "" : email.strip());
        return "forgot-username";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendPasswordReset(@RequestParam String email, Model model) {
        try {
            recoveryService.requestPasswordReset(email);
            model.addAttribute("successMessage",
                    "入力したメールアドレスが登録されている場合、パスワード再設定メールを送信しました。");
        } catch (IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        model.addAttribute("email", email == null ? "" : email.strip());
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam(required = false) String token, Model model) {
        boolean valid = recoveryService.isValidResetToken(token);
        model.addAttribute("token", token == null ? "" : token);
        model.addAttribute("tokenValid", valid);
        if (!valid) {
            model.addAttribute("errorMessage", "再設定リンクが無効または期限切れです。もう一度お手続きください。");
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String updatePassword(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            Model model) {
        try {
            recoveryService.resetPassword(token, password, passwordConfirm);
            return "redirect:/login?passwordReset";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("token", token);
            model.addAttribute("tokenValid", recoveryService.isValidResetToken(token));
            model.addAttribute("errorMessage", ex.getMessage());
            return "reset-password";
        }
    }
}
