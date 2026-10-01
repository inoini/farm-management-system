package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.MonthlyBudget;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
    Optional<MonthlyBudget> findByOwnerEmailAndBudgetYearAndBudgetMonth(String ownerEmail, Integer budgetYear, Integer budgetMonth);
    List<MonthlyBudget> findAllByOwnerEmailAndBudgetYearOrderByBudgetMonthAsc(String ownerEmail, Integer budgetYear);
    List<MonthlyBudget> findAllByOwnerEmail(String ownerEmail);
}
