package com.example.demo.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.repository.UserAccountRepository;
import com.example.demo.service.CurrentUserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * BANされたメンバーの既存セッションも次の画面操作で無効化する。
 * UserDetailsService の disabled 判定だけでは、BAN前に作成済みのHTTPセッションが
 * 残る場合があるため、認証済みリクエストでもDBの enabled を確認する。
 */
@Component
public class AccountEnabledFilter extends OncePerRequestFilter {

    private final UserAccountRepository userRepository;

    public AccountEnabledFilter(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/login")
                || path.equals("/logout")
                || path.equals("/register")
                || path.equals("/join")
                || path.equals("/forgot-username")
                || path.equals("/forgot-password")
                || path.equals("/reset-password")
                || path.equals("/health")
                || path.equals("/error")
                || path.equals("/favicon.ico")
                || path.equals("/manifest.json")
                || path.equals("/manifest.webmanifest")
                || path.equals("/service-worker.js")
                || path.equals("/pwa-check.html")
                || path.equals("/app-start.html")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/icons/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            String principal = authentication.getName() == null ? "" : authentication.getName().strip();
            var account = userRepository.findByUsernameIgnoreCase(CurrentUserService.normalizeUsername(principal))
                    .or(() -> userRepository.findByEmailIgnoreCase(CurrentUserService.normalize(principal)));

            if (account.isEmpty() || Boolean.FALSE.equals(account.get().getEnabled())) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                expireCookie(response, "JSESSIONID");
                expireCookie(response, "farm-remember-me");
                response.sendRedirect(request.getContextPath() + "/login?banned");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void expireCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
