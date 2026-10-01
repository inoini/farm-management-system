package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_contact_message")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_data_key", length = 254, nullable = false)
    private String farmDataKey;

    @Column(name = "submitter_email", length = 254, nullable = false)
    private String submitterEmail;

    @Column(name = "submitter_name", length = 100, nullable = false)
    private String submitterName;

    @Column(length = 160, nullable = false)
    private String subject;

    @Column(length = 4000, nullable = false)
    private String message;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFarmDataKey() { return farmDataKey; }
    public void setFarmDataKey(String farmDataKey) { this.farmDataKey = farmDataKey; }
    public String getSubmitterEmail() { return submitterEmail; }
    public void setSubmitterEmail(String submitterEmail) { this.submitterEmail = submitterEmail; }
    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public Boolean getEmailSent() { return emailSent; }
    public void setEmailSent(Boolean emailSent) { this.emailSent = emailSent; }
}
