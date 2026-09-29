package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "monthly_budget", uniqueConstraints = @UniqueConstraint(
        name = "uk_monthly_budget_owner_year_month",
        columnNames = {"owner_email", "budget_year", "budget_month"}))
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "budget_year", nullable = false)
    private Integer budgetYear;

    @Column(name = "budget_month", nullable = false)
    private Integer budgetMonth;

    private Double salesBudget;
    private Double expenseBudget;

    @Column(name = "owner_email", length = 254, nullable = false)
    private String ownerEmail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getBudgetYear() { return budgetYear; }
    public void setBudgetYear(Integer budgetYear) { this.budgetYear = budgetYear; }
    public Integer getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(Integer budgetMonth) { this.budgetMonth = budgetMonth; }
    public Double getSalesBudget() { return salesBudget; }
    public void setSalesBudget(Double salesBudget) { this.salesBudget = salesBudget; }
    public Double getExpenseBudget() { return expenseBudget; }
    public void setExpenseBudget(Double expenseBudget) { this.expenseBudget = expenseBudget; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
