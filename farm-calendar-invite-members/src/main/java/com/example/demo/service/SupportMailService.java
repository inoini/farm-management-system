package com.example.demo.service;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.example.demo.entity.ContactMessage;
import com.example.demo.entity.Farm;
import com.example.demo.entity.UserAccount;

@Service
public class SupportMailService {

    private static final Logger log = LoggerFactory.getLogger(SupportMailService.class);

    private final JavaMailSenderImpl mailSender;
    private final String from;
    private final String supportEmail;
    private final String mailHost;

    public SupportMailService(
            @Value("${app.mail.from:}") String from,
            @Value("${app.support.email:}") String supportEmail,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.port:587}") int mailPort,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean startTls,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:false}") boolean startTlsRequired,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}") boolean sslEnable) {
        this.from = normalize(from);
        String configuredSupportEmail = normalize(supportEmail);
        this.supportEmail = configuredSupportEmail.isBlank() ? this.from : configuredSupportEmail;
        this.mailHost = normalize(mailHost);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(this.mailHost);
        sender.setPort(mailPort);
        sender.setUsername(normalize(mailUsername));
        sender.setPassword(mailPassword == null ? "" : mailPassword);
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
        return !from.isBlank() && !supportEmail.isBlank() && !mailHost.isBlank();
    }

    public boolean send(ContactMessage contact, UserAccount account, Farm farm) {
        if (!isConfigured()) {
            log.warn("Support mail is not configured; contact message {} remains stored in DB", contact.getId());
            return false;
        }

        String farmName = farm == null || farm.getName() == null ? "未設定" : farm.getName();
        String username = account == null || account.getUsername() == null ? "未設定" : account.getUsername();
        String body = "農業管理システムからお問い合わせを受け付けました。\n\n"
                + "農場: " + farmName + "\n"
                + "ユーザー名: " + username + "\n"
                + "表示名: " + contact.getSubmitterName() + "\n"
                + "返信先: " + contact.getSubmitterEmail() + "\n"
                + "受付日時: " + contact.getSubmittedAt() + "\n\n"
                + "件名: " + contact.getSubject() + "\n\n"
                + contact.getMessage();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(supportEmail);
            if (!contact.getSubmitterEmail().isBlank()) {
                message.setReplyTo(contact.getSubmitterEmail());
            }
            message.setSubject("【農業管理システム・お問い合わせ】" + contact.getSubject());
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            log.error("Failed to send support contact mail for message {}", contact.getId(), ex);
            return false;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
