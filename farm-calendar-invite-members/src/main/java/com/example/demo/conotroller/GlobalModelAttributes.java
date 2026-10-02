package com.example.demo.conotroller;

import java.security.Principal;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.demo.entity.AppSetting;
import com.example.demo.repository.AppSettingRepository;
import com.example.demo.service.CurrentUserService;

@ControllerAdvice
public class GlobalModelAttributes {

    private final AppSettingRepository settingRepository;
    private final CurrentUserService currentUser;

    public GlobalModelAttributes(AppSettingRepository settingRepository, CurrentUserService currentUser) {
        this.settingRepository = settingRepository;
        this.currentUser = currentUser;
    }

    @ModelAttribute
    public void addSystemSettings(Model model, Principal principal) {
        AppSetting setting = new AppSetting();
        String displayFarmName = "農業管理システム";
        if (principal != null) {
            try {
                var account = currentUser.account();
                String dataKey = currentUser.email();
                setting = settingRepository.findFirstByOwnerEmail(dataKey).orElse(setting);
                model.addAttribute("loginUserName", account.getDisplayName());
                model.addAttribute("loginUsername", currentUser.username());
                model.addAttribute("loginUserEmail", account.getEmail());
                model.addAttribute("loginFarmRole", account.getFarmRole());
                if (account.getFarm() != null && account.getFarm().getName() != null
                        && !account.getFarm().getName().isBlank()) {
                    displayFarmName = account.getFarm().getName();
                }
            } catch (RuntimeException ex) {
                String safePrincipalName = safePrincipalUsername(principal.getName());
                model.addAttribute("loginUserName", safePrincipalName);
                model.addAttribute("loginUsername", safePrincipalName);
                model.addAttribute("loginUserEmail", "");
                model.addAttribute("loginFarmRole", "MEMBER");
            }
        }
        if (setting.getFarmName() != null && !setting.getFarmName().isBlank()) {
            displayFarmName = setting.getFarmName();
        }
        model.addAttribute("farmName", displayFarmName);
        model.addAttribute("notificationsEnabled", !Boolean.FALSE.equals(setting.getNotificationsEnabled()));
        model.addAttribute("uiTheme", validTheme(setting.getUiTheme()));
        model.addAttribute("layoutMode", validLayout(setting.getLayoutMode()));
    }

    private String safePrincipalUsername(String principalName) {
        String value = principalName == null ? "" : principalName.strip();
        if (value.isBlank() || value.contains("@")) {
            return "user";
        }
        return value;
    }

    private String validTheme(String value) {
        return switch (value == null ? "" : value) {
            case "blue", "earth", "discord" -> value;
            default -> "green";
        };
    }

    private String validLayout(String value) {
        return switch (value == null ? "" : value) {
            case "compact", "wide", "sidebar", "calendar2" -> value;
            default -> "sidebar";
        };
    }
}
