package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Crop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cropName;

    private String variety;

    private String fieldName;

    private LocalDate plantingDate;

    private LocalDate harvestDate;

    private Double area;

    // 収穫予定量（kg）
    private Double expectedHarvestKg;

    private String status;

    // 天候を反映した収穫目安（登録済み収穫予定日は残したまま別保存）
    private LocalDate weatherAdjustedHarvestDate;

    @Column(length = 2500)
    private String weatherAdvice;

    private LocalDateTime weatherAnalyzedAt;


    public Crop() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public LocalDate getPlantingDate() {
        return plantingDate;
    }

    public void setPlantingDate(LocalDate plantingDate) {
        this.plantingDate = plantingDate;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Double getExpectedHarvestKg() {
        return expectedHarvestKg;
    }

    public void setExpectedHarvestKg(Double expectedHarvestKg) {
        this.expectedHarvestKg = expectedHarvestKg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public LocalDate getWeatherAdjustedHarvestDate() {
        return weatherAdjustedHarvestDate;
    }

    public void setWeatherAdjustedHarvestDate(LocalDate weatherAdjustedHarvestDate) {
        this.weatherAdjustedHarvestDate = weatherAdjustedHarvestDate;
    }

    public String getWeatherAdvice() {
        return weatherAdvice;
    }

    public void setWeatherAdvice(String weatherAdvice) {
        this.weatherAdvice = weatherAdvice;
    }

    public LocalDateTime getWeatherAnalyzedAt() {
        return weatherAnalyzedAt;
    }

    public void setWeatherAnalyzedAt(LocalDateTime weatherAnalyzedAt) {
        this.weatherAnalyzedAt = weatherAnalyzedAt;
    }

    /** Login e-mail that owns this record. Null values are legacy pre-multi-user data. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.Column(name = "owner_email", length = 254)
    private String ownerEmail;

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
