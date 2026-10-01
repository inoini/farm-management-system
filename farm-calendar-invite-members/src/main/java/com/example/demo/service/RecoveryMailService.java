package com.example.demo.service;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.example.demo.entity.UserAccount;

@Service
public class RecoveryMailService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryMailService.class);

    private final JavaMailSenderImpl mailSender;
    private final String from;
    private final String mailHost;
    private final String mailUsername;
    private final String mailPassword;
    private final String baseUrl;

    public RecoveryMailService(
            @Value("${app.mail.from:}") String from,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.port:587}") int mailPort,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean startTls,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:false}") boolean startTlsRequired,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}") boolean sslEnable,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.from = normalize(from);
        this.mailHost = normalize(mailHost);
        this.mailUsername = normalize(mailUsername);
        this.mailPassword = mailPassword == null ? "" : mailPassword.replaceAll("\\s+", "");
        this.baseUrl = baseUrl == null ? "http://localhost:8080" : baseUrl.strip().replaceAll("/+$", "");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(this.mailHost);
        sender.setPort(mailPort);
        sender.setUsername(this.mailUsername);
        sender.setPassword(this.mailPassword);
        sender.setDefaultEncoding("UTF-8");
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", Boolean.toString(smtpAuth));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        properties.put("mail.smtp.starttls.required", Boolean.toString(startTlsRequired));
        properties.put("mail.smtp.ssl.enable", Boolean.toString(sslEnable));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        this.mailSender = sender;
    }

    public boolean isConfigured() {
        return !from.isBlank()
                && !mailHost.isBlank()
                && !mailUsername.isBlank()
                && !mailPassword.isBlank();
    }

    public boolean sendUsername(UserAccount account) {
        String body = "農業管理システムのユーザー名は次のとおりです。\n\n"
                + account.getUsername() + "\n\n"
                + accountLabel(account)
                + "このメールに心当たりがない場合は、そのまま破棄してください。";
        return send(account.getEmail(), "【農業管理システム】ユーザー名のお知らせ", body);
    }

    public boolean sendPasswordReset(UserAccount account, String rawToken) {
        String link = baseUrl + "/reset-password?token=" + rawToken;
        String body = "農業管理システムのパスワード再設定を受け付けました。\n\n"
                + "対象ユーザー名: " + safe(account.getUsername()) + "\n"
                + accountLabel(account)
                + "次のリンクから30分以内に新しいパスワードを設定してください。\n"
                + link + "\n\n"
                + "このメールに心当たりがない場合は、リンクを開かず破棄してください。";
        return send(account.getEmail(), "【農業管理システム】パスワード再設定", body);
    }

    private String accountLabel(UserAccount account) {
        String farmName = account.getFarm() == null ? "" : safe(account.getFarm().getName());
        String role = "OWNER".equalsIgnoreCase(account.getFarmRole()) ? "管理者" : "メンバー";
        if (farmName.isBlank()) {
            return "権限: " + role + "\n\n";
        }
        return "農場: " + farmName + "\n権限: " + role + "\n\n";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean send(String to, String subject, String body) {
        if (!isConfigured()) {
            log.error("Recovery mail is not configured: sender/host/username/password is incomplete");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Recovery mail sent successfully");
            return true;
        } catch (MailException ex) {
            log.error("Failed to send recovery mail", ex);
            return false;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
