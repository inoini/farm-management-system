package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.repository.UserAccountRepository;
import com.example.demo.service.CurrentUserService;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(UserAccountRepository userRepository) {
        return loginId -> userRepository.findByUsernameIgnoreCase(CurrentUserService.normalizeUsername(loginId))
                .or(() -> userRepository.findByEmailIgnoreCase(CurrentUserService.normalize(loginId)))
                .map(account -> User.withUsername(account.getUsername() == null || account.getUsername().isBlank()
                                ? account.getEmail() : account.getUsername())
                        .password(account.getPasswordHash())
                        .roles(account.getRole() == null || account.getRole().isBlank() ? "USER" : account.getRole())
                        .disabled(Boolean.FALSE.equals(account.getEnabled()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません。"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/register", "/join", "/forgot-username", "/forgot-password", "/reset-password",
                    "/health", "/error", "/favicon.ico",
                    "/manifest.json", "/manifest.webmanifest", "/service-worker.js",
                    "/css/**", "/js/**", "/images/**", "/icons/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        return http.build();
    }
}
