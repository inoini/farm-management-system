package com.example.demo.security;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.repository.UserAccountRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.FarmInviteService;

@Controller
public class FarmMembershipController {

    private final CurrentUserService currentUser;
    private final UserAccountRepository userRepository;
    private final FarmInviteService inviteService;

    public FarmMembershipController(CurrentUserService currentUser,
            UserAccountRepository userRepository, FarmInviteService inviteService) {
        this.currentUser = currentUser;
        this.userRepository = userRepository;
        this.inviteService = inviteService;
    }

    @GetMapping("/farm/members")
    public String members(Model model) {
        var account = currentUser.account();
        var farm = currentUser.farm();
        model.addAttribute("sharedFarm", farm);
        model.addAttribute("members", userRepository.findAllByFarmIdOrderByCreatedAtAsc(farm.getId()));
        model.addAttribute("farmOwner", "OWNER".equalsIgnoreCase(account.getFarmRole()));
        return "farm-members";
    }

    @PostMapping("/farm/invite")
    public String createInvite(RedirectAttributes redirect) {
        try {
            var result = inviteService.createInvite();
            redirect.addFlashAttribute("inviteCode", result.code());
            redirect.addFlashAttribute("inviteExpiresAt", result.expiresAt());
            redirect.addFlashAttribute("successMessage", "招待コードを発行しました。7日間有効です。");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/farm/members";
    }

    @PostMapping("/farm/invite/revoke-all")
    public String revokeInvites(RedirectAttributes redirect) {
        try {
            inviteService.revokeAll();
            redirect.addFlashAttribute("successMessage", "有効な招待コードをすべて無効にしました。");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/farm/members";
    }
}
