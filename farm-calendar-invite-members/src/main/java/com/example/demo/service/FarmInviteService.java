package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Farm;
import com.example.demo.entity.FarmInvite;
import com.example.demo.repository.FarmInviteRepository;
import com.example.demo.repository.FarmRepository;

@Service
public class FarmInviteService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final FarmInviteRepository inviteRepository;
    private final FarmRepository farmRepository;
    private final CurrentUserService currentUser;

    public FarmInviteService(FarmInviteRepository inviteRepository, FarmRepository farmRepository,
            CurrentUserService currentUser) {
        this.inviteRepository = inviteRepository;
        this.farmRepository = farmRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public InvitationResult createInvite() {
        if (!currentUser.isFarmOwner()) {
            throw new IllegalStateException("招待コードを発行できるのは農場の管理者だけです。");
        }
        Farm farm = currentUser.farm();
        String raw = generateCode();
        FarmInvite invite = new FarmInvite();
        invite.setFarmId(farm.getId());
        invite.setCodeHash(hash(normalizeCode(raw)));
        invite.setCreatedAt(LocalDateTime.now());
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite.setRevoked(false);
        inviteRepository.save(invite);
        return new InvitationResult(raw, invite.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public Farm resolveFarmForJoin(String rawCode) {
        String normalized = normalizeCode(rawCode);
        if (normalized.length() != 12) {
            throw new IllegalArgumentException("招待コードを確認してください。");
        }
        FarmInvite invite = inviteRepository.findByCodeHashAndRevokedFalse(hash(normalized))
                .filter(v -> v.getExpiresAt() != null && v.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("招待コードが無効または期限切れです。農場の管理者に新しいコードを発行してもらってください。"));
        return farmRepository.findById(invite.getFarmId())
                .orElseThrow(() -> new IllegalArgumentException("招待先の農場が見つかりません。"));
    }

    @Transactional
    public void revokeAll() {
        if (!currentUser.isFarmOwner()) {
            throw new IllegalStateException("招待コードを無効化できるのは農場の管理者だけです。");
        }
        Long farmId = currentUser.farm().getId();
        for (FarmInvite invite : inviteRepository.findAllByFarmIdAndRevokedFalseOrderByCreatedAtDesc(farmId)) {
            invite.setRevoked(true);
            inviteRepository.save(invite);
        }
    }

    private String generateCode() {
        StringBuilder plain = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            plain.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return plain.substring(0, 4) + "-" + plain.substring(4, 8) + "-" + plain.substring(8, 12);
    }

    public static String normalizeCode(String raw) {
        return raw == null ? "" : raw.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record InvitationResult(String code, LocalDateTime expiresAt) {}
}
