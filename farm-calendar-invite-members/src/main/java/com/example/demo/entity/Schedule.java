package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 日付
    private String date;

    // 開始時間
    private String startTime;

    // 終了時間
    private String endTime;

    // 担当者
    private String userName;

    // 作業内容
    private String schedule;

    // 圃場名
    private String fieldName;

    // 作物
    private String cropName;

    // 作業種類
    private String workType;

    // 進捗状態（未着手・進行中・完了）
    private String status = "未着手";

    // メモ
    private String memo;

    // ==========================
    // 使用資材
    // ==========================
    private String itemName;

    // 使用数量
    private Integer useQuantity;

    // ==========================
    // Getter
    // ==========================
    
    public Long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getUserName() {
        return userName;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getCropName() {
        return cropName;
    }

    public String getWorkType() {
        return workType;
    }

    public String getStatus() {
        return status;
    }

    public String getMemo() {
        return memo;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getUseQuantity() {
        return useQuantity;
    }

    // ==========================
    // Setter
    // ==========================

    public void setId(Long id) {
        this.id = id;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setUseQuantity(Integer useQuantity) {
        this.useQuantity = useQuantity;
    }


    /** Login e-mail that owns this record. Null values are legacy pre-multi-user data. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.Column(name = "owner_email", length = 254)
    private String ownerEmail;

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
