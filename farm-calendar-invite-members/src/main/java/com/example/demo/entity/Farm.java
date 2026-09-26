package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "farm_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_farm_account_data_key", columnNames = "data_key")
})
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Existing owner_email columns use this value as the shared farm data key.
     * Keeping the storage key separate from a person's email lets several users
     * safely share one farm without sharing login credentials.
     */
    @Column(name = "data_key", nullable = false, length = 254)
    private String dataKey;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataKey() { return dataKey; }
    public void setDataKey(String dataKey) { this.dataKey = dataKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
