package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_app_setting", uniqueConstraints = @UniqueConstraint(name = "uk_user_app_setting_owner", columnNames = "owner_email"))
public class AppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String farmName = "農業管理システム";
    private Double monthlyHarvestTargetKg = 1000.0;
    private Boolean notificationsEnabled = true;
    private String uiTheme = "green";
    private String layoutMode = "sidebar";

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "owner_email", length = 254)
    private String ownerEmail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public Double getMonthlyHarvestTargetKg() { return monthlyHarvestTargetKg; }
    public void setMonthlyHarvestTargetKg(Double monthlyHarvestTargetKg) { this.monthlyHarvestTargetKg = monthlyHarvestTargetKg; }
    public Boolean getNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public String getUiTheme() { return uiTheme; }
    public void setUiTheme(String uiTheme) { this.uiTheme = uiTheme; }
    public String getLayoutMode() { return layoutMode; }
    public void setLayoutMode(String layoutMode) { this.layoutMode = layoutMode; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
