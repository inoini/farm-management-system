package com.example.demo.entity;


import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;


@Entity
public class Sales {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private LocalDate date;


    private String crop;


    private Double amount;


    @Column(columnDefinition="TEXT")
    private String memo;



    public Long getId(){
        return id;
    }


    public void setId(Long id){
        this.id = id;
    }


    public LocalDate getDate(){
        return date;
    }


    public void setDate(LocalDate date){
        this.date=date;
    }


    public String getCrop(){
        return crop;
    }


    public void setCrop(String crop){
        this.crop=crop;
    }


    public Double getAmount(){
        return amount;
    }


    public void setAmount(Double amount){
        this.amount=amount;
    }


    public String getMemo(){
        return memo;
    }


    public void setMemo(String memo){
        this.memo=memo;
    }


    @Column(precision = 12, scale = 3)
    private BigDecimal baseWeightKg;
    @Column(precision = 12, scale = 0)
    private BigDecimal basePrice;
    private Integer packageCount;

    public BigDecimal getBaseWeightKg() { return baseWeightKg; }
    public void setBaseWeightKg(BigDecimal value) { baseWeightKg = value; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal value) { basePrice = value; }
    public Integer getPackageCount() { return packageCount; }
    public void setPackageCount(Integer value) { packageCount = value; }

    public BigDecimal getTotalWeightKg() {
        return baseWeightKg == null || packageCount == null ? null
                : baseWeightKg.multiply(BigDecimal.valueOf(packageCount));
    }

    public String getPriceDescription() {
        if (baseWeightKg == null || basePrice == null) return "—";
        return baseWeightKg.stripTrailingZeros().toPlainString() + "kgあたり"
                + String.format(java.util.Locale.JAPAN, "%,.0f", basePrice) + "円";
    }

    // The server calculates the amount independently of the browser.
    public void calculateAndValidateAmount() {
        if (baseWeightKg == null && basePrice == null && packageCount == null) {
            if (amount == null || !Double.isFinite(amount) || amount < 0) {
                throw new IllegalArgumentException("売上金額を0円以上で入力してください。");
            }
            return;
        }
        if (baseWeightKg == null || basePrice == null || packageCount == null) {
            throw new IllegalArgumentException("何kgあたり・その重量の価格・販売数量をすべて入力してください。");
        }
        if (baseWeightKg.signum() <= 0 || baseWeightKg.compareTo(new BigDecimal("999999999.999")) > 0
                || baseWeightKg.stripTrailingZeros().scale() > 3) {
            throw new IllegalArgumentException("重量は0.001～999999999.999kg、小数点以下3桁までで入力してください。");
        }
        if (basePrice.signum() < 0 || basePrice.compareTo(new BigDecimal("999999999999")) > 0
                || basePrice.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("価格は0～999999999999円の整数で入力してください。");
        }
        if (packageCount < 1 || packageCount > 1000000) {
            throw new IllegalArgumentException("販売数量は1～1000000セットの整数で入力してください。");
        }
        BigDecimal total = basePrice.multiply(BigDecimal.valueOf(packageCount));
        if (total.compareTo(new BigDecimal("999999999999")) > 0) {
            throw new IllegalArgumentException("売上金額の合計は999999999999円以下にしてください。");
        }
        amount = total.doubleValue();
    }

    /** Login e-mail that owns this record. Null values are legacy pre-multi-user data. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.Column(name = "owner_email", length = 254)
    private String ownerEmail;

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
