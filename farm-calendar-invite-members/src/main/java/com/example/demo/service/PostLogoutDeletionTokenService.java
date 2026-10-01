package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class PostLogoutDeletionTokenService {

    private static final String COOKIE_NAME = "farm-delete-account";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final byte[] secret;

    public PostLogoutDeletionTokenService(
            @Value("${app.security.remember-me-key}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public void issue(HttpServletResponse response, Long accountId) {
        long expiresAt = Instant.now().plus(TTL).getEpochSecond();
        String payload = accountId + ":" + expiresAt;
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + sign(payload);
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(TTL)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<Long> resolveAccountId(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        for (Cookie cookie : request.getCookies()) {
            if (!COOKIE_NAME.equals(cookie.getName())) continue;
            try {
                String[] parts = cookie.getValue().split("\\.", 2);
                if (parts.length != 2) return Optional.empty();
                String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                if (!constantTimeEquals(sign(payload), parts[1])) return Optional.empty();
                String[] values = payload.split(":", 2);
                long accountId = Long.parseLong(values[0]);
                long expiresAt = Long.parseLong(values[1]);
                if (Instant.now().getEpochSecond() > expiresAt) return Optional.empty();
                return Optional.of(accountId);
            } catch (RuntimeException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign account deletion token", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(a, b);
    }
}
