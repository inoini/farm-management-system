package com.example.demo.entity;

import java.time.LocalDate;

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

    // カテゴリ（肥料・農薬・種・苗・その他）
    private String category;

    // 在庫数量
    private Integer quantity;

    // 単位
    private String unit;

    // 最低在庫
    private Integer minimumStock;

    // 保管場所
    private String location;

    // 対象作物（農薬・肥料共通）
    private String targetCrop;

    // 農薬：希釈倍率（例：1000倍）
    private String dilutionRate;

    // 農薬：登録上の最大使用回数
    private Integer maxUsageCount;

    // 農薬：現在までの使用回数
    private Integer usedCount;

    // 農薬：収穫前日数
    private Integer preHarvestDays;

    // 肥料：成分・配合（例：N-P-K 8-8-8）
    private String fertilizerComposition;

    // 最終使用日
    private LocalDate lastUsedDate;

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

    public String getTargetCrop() {
        return targetCrop;
    }

    public String getDilutionRate() {
        return dilutionRate;
    }

    public Integer getMaxUsageCount() {
        return maxUsageCount;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public Integer getPreHarvestDays() {
        return preHarvestDays;
    }

    public String getFertilizerComposition() {
        return fertilizerComposition;
    }

    public LocalDate getLastUsedDate() {
        return lastUsedDate;
    }

    public String getMemo() {
        return memo;
    }

    public Integer getRemainingUsageCount() {
        if (maxUsageCount == null) {
            return null;
        }
        int used = usedCount == null ? 0 : usedCount;
        return Math.max(0, maxUsageCount - used);
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

    public void setTargetCrop(String targetCrop) {
        this.targetCrop = targetCrop;
    }

    public void setDilutionRate(String dilutionRate) {
        this.dilutionRate = dilutionRate;
    }

    public void setMaxUsageCount(Integer maxUsageCount) {
        this.maxUsageCount = maxUsageCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public void setPreHarvestDays(Integer preHarvestDays) {
        this.preHarvestDays = preHarvestDays;
    }

    public void setFertilizerComposition(String fertilizerComposition) {
        this.fertilizerComposition = fertilizerComposition;
    }

    public void setLastUsedDate(LocalDate lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
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
