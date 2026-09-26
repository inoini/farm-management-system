package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.UserAccount;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserAccountRepository;

@Service
public class AccountRecoveryService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecoveryMailService mailService;

    public AccountRecoveryService(UserAccountRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            RecoveryMailService mailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    /**
     * 常に同じ画面応答にできるよう、アカウントが存在しない場合も正常終了する。
     */
    @Transactional(readOnly = true)
    public void requestUsernameReminder(String email) {
        ensureMailConfigured();
        userRepository.findByEmailIgnoreCase(CurrentUserService.normalize(email))
                .filter(user -> user.getUsername() != null && !user.getUsername().isBlank())
                .ifPresent(mailService::sendUsername);
    }

    /**
     * 常に同じ画面応答にできるよう、アカウントが存在しない場合も正常終了する。
     */
    @Transactional
    public void requestPasswordReset(String email) {
        ensureMailConfigured();
        userRepository.findByEmailIgnoreCase(CurrentUserService.normalize(email)).ifPresent(user -> {
            tokenRepository.deleteAllByUserId(user.getId());
            String rawToken = newToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hash(rawToken));
            token.setCreatedAt(LocalDateTime.now());
            token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            tokenRepository.save(token);
            mailService.sendPasswordReset(user, rawToken);
        });
    }

    @Transactional(readOnly = true)
    public boolean isValidResetToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;
        return tokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public void resetPassword(String rawToken, String password, String passwordConfirm) {
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new IllegalArgumentException("パスワードは8～64文字で入力してください。");
        }
        if (!password.equals(passwordConfirm)) {
            throw new IllegalArgumentException("確認用パスワードが一致しません。");
        }

        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken == null ? "" : rawToken))
                .filter(v -> v.getUsedAt() == null)
                .filter(v -> v.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("再設定リンクが無効または期限切れです。もう一度お手続きください。"));

        UserAccount user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("アカウントが見つかりません。"));
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private void ensureMailConfigured() {
        if (!mailService.isConfigured()) {
            throw new IllegalStateException("現在、メールによるアカウント復旧を利用できません。管理者にお問い合わせください。");
        }
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
