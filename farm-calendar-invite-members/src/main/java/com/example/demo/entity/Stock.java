package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 資材名
    private String itemName;

    // カテゴリ
    private String category;

    // 在庫数量
    private Integer quantity;

    // 単位
    private String unit;

    // 最低在庫
    private Integer minimumStock;

    // 保管場所
    private String location;

    // 備考
    private String memo;

    // ==========================
    // Getter
    // ==========================

    public Long getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategory() {
        return category;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getMinimumStock() {
        return minimumStock;
    }

    public String getLocation() {
        return location;
    }

    public String getMemo() {
        return memo;
    }

    // ==========================
    // Setter
    // ==========================

    public void setId(Long id) {
        this.id = id;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setMinimumStock(Integer minimumStock) {
        this.minimumStock = minimumStock;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }


    /** Login e-mail that owns this record. Null values are legacy pre-multi-user data. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.Column(name = "owner_email", length = 254)
    private String ownerEmail;

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}