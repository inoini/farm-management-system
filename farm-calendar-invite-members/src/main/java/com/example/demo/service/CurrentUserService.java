package com.example.demo.service;

import java.util.Locale;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Farm;
import com.example.demo.entity.UserAccount;
import com.example.demo.repository.UserAccountRepository;

@Service
public class CurrentUserService {

    private final UserAccountRepository userRepository;

    public CurrentUserService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    private String principalName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("ログインユーザーを取得できません。");
        }
        return authentication.getName() == null ? "" : authentication.getName().strip();
    }

    public UserAccount account() {
        String principal = principalName();
        return userRepository.findByUsernameIgnoreCase(normalizeUsername(principal))
                .orElseThrow(() -> new IllegalStateException("ログインユーザーが見つかりません。"));
    }

    public Farm farm() {
        Farm farm = account().getFarm();
        if (farm == null) {
            throw new IllegalStateException("農場情報が設定されていません。管理者にお問い合わせください。");
        }
        return farm;
    }

    /**
     * 既存コードとの互換性のためメソッド名は email() のままだが、
     * 返す値は個人メールではなく農場共通のデータ所有キー。
     */
    public String email() {
        Farm farm = account().getFarm();
        if (farm != null && farm.getDataKey() != null && !farm.getDataKey().isBlank()) {
            return farm.getDataKey();
        }
        return normalize(account().getEmail());
    }

    public String username() {
        UserAccount account = account();
        if (account.getUsername() == null || account.getUsername().isBlank()) {
            return account.getEmail();
        }
        return account.getUsername();
    }

    public boolean isFarmOwner() {
        return "OWNER".equalsIgnoreCase(account().getFarmRole());
    }

    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    public static String normalizeUsername(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }
}
