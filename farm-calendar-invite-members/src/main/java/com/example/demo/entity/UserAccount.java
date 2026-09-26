package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "app_user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_app_user_username", columnNames = "username")
})
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40)
    private String username;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    @Column(nullable = false)
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Column(length = 20)
    private String farmRole = "MEMBER";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public String getFarmRole() { return farmRole; }
    public void setFarmRole(String farmRole) { this.farmRole = farmRole; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
