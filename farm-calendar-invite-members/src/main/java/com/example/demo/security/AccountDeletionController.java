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
import com.example.demo.service.PostLogoutDeletionTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AccountDeletionController {

    private final CurrentUserService currentUser;
    private final AccountDeletionService deletionService;
    private final PostLogoutDeletionTokenService postLogoutTokenService;

    public AccountDeletionController(CurrentUserService currentUser,
            AccountDeletionService deletionService,
            PostLogoutDeletionTokenService postLogoutTokenService) {
        this.currentUser = currentUser;
        this.deletionService = deletionService;
        this.postLogoutTokenService = postLogoutTokenService;
    }

    @GetMapping("/account/delete")
    public String deleteAccount(Model model) {
        populate(model, currentUser.account().getId(), false);
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
            populate(model, currentUser.account().getId(), false);
            return "account-delete";
        }
    }

    @GetMapping("/account/delete-after-logout")
    public String deleteAfterLogout(HttpServletRequest request, Model model) {
        Long accountId = postLogoutTokenService.resolveAccountId(request).orElse(null);
        if (accountId == null) {
            return "redirect:/login?logout&deleteExpired";
        }
        try {
            populate(model, accountId, true);
            return "account-delete";
        } catch (IllegalArgumentException ex) {
            return "redirect:/login?logout&deleteExpired";
        }
    }

    @PostMapping("/account/delete-after-logout")
    public String deleteAfterLogout(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) Long successorMemberId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        Long accountId = postLogoutTokenService.resolveAccountId(request).orElse(null);
        if (accountId == null) {
            return "redirect:/login?logout&deleteExpired";
        }
        try {
            deletionService.deleteAccount(accountId, email, password, successorMemberId);
            postLogoutTokenService.clear(response);
            return "redirect:/login?deleted";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("enteredEmail", email == null ? "" : email.strip());
            try {
                populate(model, accountId, true);
                return "account-delete";
            } catch (IllegalArgumentException missing) {
                postLogoutTokenService.clear(response);
                return "redirect:/login?deleted";
            }
        }
    }

    private void populate(Model model, Long accountId, boolean afterLogout) {
        UserAccount account = deletionService.account(accountId);
        boolean owner = "OWNER".equalsIgnoreCase(account.getFarmRole());
        model.addAttribute("account", account);
        model.addAttribute("farmOwner", owner);
        model.addAttribute("hasOtherMembers", owner && deletionService.hasOtherMembers(accountId));
        model.addAttribute("successorCandidates", owner ? deletionService.successorCandidates(accountId) : java.util.List.of());
        model.addAttribute("deleteAfterLogout", afterLogout);
    }
}
