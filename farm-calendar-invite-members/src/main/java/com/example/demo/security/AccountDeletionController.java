package com.example.demo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.UserAccount;
import com.example.demo.service.AccountDeletionService;
import com.example.demo.service.CurrentUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AccountDeletionController {

    private final CurrentUserService currentUser;
    private final AccountDeletionService deletionService;

    public AccountDeletionController(CurrentUserService currentUser, AccountDeletionService deletionService) {
        this.currentUser = currentUser;
        this.deletionService = deletionService;
    }

    @GetMapping("/account/delete")
    public String deleteAccount(Model model) {
        populate(model);
        return "account-delete";
    }

    @PostMapping("/account/delete")
    public String deleteAccount(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) Long successorMemberId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        try {
            deletionService.deleteCurrentAccount(email, password, successorMemberId);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            new CookieClearingLogoutHandler("JSESSIONID", "farm-remember-me")
                    .logout(request, response, authentication);
            return "redirect:/login?deleted";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("enteredEmail", email == null ? "" : email.strip());
            populate(model);
            return "account-delete";
        }
    }

    private void populate(Model model) {
        UserAccount account = currentUser.account();
        boolean owner = "OWNER".equalsIgnoreCase(account.getFarmRole());
        model.addAttribute("account", account);
        model.addAttribute("farmOwner", owner);
        model.addAttribute("hasOtherMembers", owner && deletionService.hasOtherMembers());
        model.addAttribute("successorCandidates", owner ? deletionService.successorCandidates() : java.util.List.of());
    }
}
