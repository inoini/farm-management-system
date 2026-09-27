package com.example.demo.security;

import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.UserAccount;
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
        model.addAttribute("currentMemberId", account.getId());
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

    @PostMapping("/farm/members/{memberId}/ban")
    public String banMember(@PathVariable Long memberId, RedirectAttributes redirect) {
        return updateMemberAccess(memberId, false, redirect);
    }

    @PostMapping("/farm/members/{memberId}/unban")
    public String unbanMember(@PathVariable Long memberId, RedirectAttributes redirect) {
        return updateMemberAccess(memberId, true, redirect);
    }

    private String updateMemberAccess(Long memberId, boolean enabled, RedirectAttributes redirect) {
        try {
            UserAccount actor = currentUser.account();
            if (!"OWNER".equalsIgnoreCase(actor.getFarmRole())) {
                throw new IllegalStateException("メンバーのBAN・解除を行えるのは農場の管理者だけです。");
            }

            UserAccount target = userRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalStateException("対象のメンバーが見つかりません。"));

            if (actor.getFarm() == null || target.getFarm() == null
                    || !Objects.equals(actor.getFarm().getId(), target.getFarm().getId())) {
                throw new IllegalStateException("別の農場のメンバーは操作できません。");
            }
            if (Objects.equals(actor.getId(), target.getId())) {
                throw new IllegalStateException("自分自身をBANすることはできません。");
            }
            if ("OWNER".equalsIgnoreCase(target.getFarmRole())) {
                throw new IllegalStateException("農場の管理者をBANすることはできません。");
            }

            target.setEnabled(enabled);
            userRepository.save(target);

            String name = target.getDisplayName() == null || target.getDisplayName().isBlank()
                    ? target.getUsername() : target.getDisplayName();
            redirect.addFlashAttribute("successMessage",
                    enabled ? name + "さんのBANを解除しました。" : name + "さんをBANしました。ログインできなくなります。");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/farm/members";
    }
}
