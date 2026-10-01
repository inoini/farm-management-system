package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;

import com.example.demo.repository.UserAccountRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.PostLogoutDeletionTokenService;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService users(UserAccountRepository userRepository) {
        return loginId -> userRepository.findByUsernameIgnoreCase(CurrentUserService.normalizeUsername(loginId))
                .map(account -> User.withUsername(account.getUsername())
                        .password(account.getPasswordHash())
                        .roles(account.getRole() == null || account.getRole().isBlank() ? "USER" : account.getRole())
                        .disabled(Boolean.FALSE.equals(account.getEnabled()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません。"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            UserDetailsService userDetailsService,
            AccountEnabledFilter accountEnabledFilter,
            UserAccountRepository userRepository,
            PostLogoutDeletionTokenService postLogoutDeletionTokenService,
            @org.springframework.beans.factory.annotation.Value("${app.security.remember-me-key}") String rememberMeKey)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/robots.txt", "/sitemap.xml",
                    "/login", "/register", "/join", "/forgot-username", "/forgot-password", "/reset-password",
                    "/account/delete-after-logout",
                    "/auth/csrf", "/health", "/error", "/favicon.ico", "/pwa-check.html", "/app-start.html",
                    "/manifest.json", "/manifest.webmanifest", "/service-worker.js",
                    "/css/**", "/js/**", "/images/**", "/icons/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login").loginProcessingUrl("/login")
                .usernameParameter("username").passwordParameter("password")
                .defaultSuccessUrl("/calendar", true)
                .failureHandler((request, response, exception) -> {
                    boolean disabled = exception instanceof DisabledException || exception.getCause() instanceof DisabledException;
                    response.sendRedirect(request.getContextPath() + (disabled ? "/login?banned" : "/login?error"));
                }).permitAll()
            )
            .rememberMe(remember -> remember
                .key(rememberMeKey).userDetailsService(userDetailsService)
                .rememberMeCookieName("farm-remember-me")
                .tokenValiditySeconds(60 * 60 * 24 * 30).alwaysRemember(true).useSecureCookie(true)
            )
            .addFilterAfter(accountEnabledFilter, RememberMeAuthenticationFilter.class)
            .logout(logout -> logout
                .logoutUrl("/logout")
                .successHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        userRepository.findByUsernameIgnoreCase(CurrentUserService.normalizeUsername(authentication.getName()))
                                .ifPresent(account -> postLogoutDeletionTokenService.issue(response, account.getId()));
                    }
                    response.sendRedirect(request.getContextPath() + "/login?logout");
                })
                .invalidateHttpSession(true).clearAuthentication(true)
                .deleteCookies("JSESSIONID", "farm-remember-me").permitAll()
            );
        return http.build();
    }
}
