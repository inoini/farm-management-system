package com.example.demo.entity;


import jakarta.persistence.*;
import java.time.LocalDate;


@Entity
public class Expense {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private LocalDate date;


    private String category;


    private Double amount;


    @Column(columnDefinition = "TEXT")
    private String memo;



    public Long getId() {
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }
    public LocalDate getDate() {
        return date;
    }


    public void setDate(LocalDate date) {
        this.date = date;
    }


    public String getCategory() {
        return category;
    }


    public void setCategory(String category) {
        this.category = category;
    }


    public Double getAmount() {
        return amount;
    }


    public void setAmount(Double amount) {
        this.amount = amount;
    }


    public String getMemo() {
        return memo;
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