package com.example.demo.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "harvest_shipment")
public class HarvestShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate workDate;
    private String cropName;
    private String fieldName;
    private Double harvestKg;
    private Double shippedKg;
    private String destination;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String memo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public Double getHarvestKg() { return harvestKg; }
    public void setHarvestKg(Double harvestKg) { this.harvestKg = harvestKg; }
    public Double getShippedKg() { return shippedKg; }
    public void setShippedKg(Double shippedKg) { this.shippedKg = shippedKg; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    /** Login e-mail that owns this record. Null values are legacy pre-multi-user data. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.Column(name = "owner_email", length = 254)
    private String ownerEmail;

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
