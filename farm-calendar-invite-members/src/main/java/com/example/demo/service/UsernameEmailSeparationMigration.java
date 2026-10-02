package com.example.demo.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.UserAccount;
import com.example.demo.repository.UserAccountRepository;

/**
 * 旧データでユーザー名にメールアドレスそのものが保存されている場合、
 * 表示名を基準にユーザー名を作り直す。
 * 新規登録では RegistrationService がユーザー名とメールアドレスを別々に保存するため、
 * この処理は既存データの修復専用。
 */
@Component
@Order(100)
public class UsernameEmailSeparationMigration implements ApplicationRunner {

    private final UserAccountRepository userRepository;

    public UsernameEmailSeparationMigration(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (UserAccount user : userRepository.findAll()) {
            if (!needsRepair(user)) {
                continue;
            }

            String base = usernameBaseFromDisplayName(user.getDisplayName());
            user.setUsername(uniqueUsername(CurrentUserService.normalizeUsername(base)));
            userRepository.save(user);
        }
    }

    private boolean needsRepair(UserAccount user) {
        String username = CurrentUserService.normalizeUsername(user.getUsername());
        if (username.isBlank()) {
            return false;
        }

        String email = CurrentUserService.normalize(user.getEmail());
        return username.contains("@") || (!email.isBlank() && username.equalsIgnoreCase(email));
    }

    private String usernameBaseFromDisplayName(String displayName) {
        String base = displayName == null ? "" : displayName.strip();
        base = base.replaceAll("[^\\p{L}\\p{N}._-]", "-");
        base = base.replaceAll("-+", "-");
        base = base.replaceAll("^-+|-+$", "");
        if (base.length() < 3) {
            base = "farm-" + base;
        }
        if (base.length() > 32) {
            base = base.substring(0, 32);
        }
        if (base.isBlank()) {
            base = "farm-user";
        }
        return base;
    }

    private String uniqueUsername(String requested) {
        String base = requested == null || requested.isBlank() ? "farm-user" : requested;
        String candidate = base;
        int suffix = 2;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            String tail = "-" + suffix++;
            int maxBase = Math.max(1, 40 - tail.length());
            candidate = (base.length() > maxBase ? base.substring(0, maxBase) : base) + tail;
        }
        return candidate;
    }
}
