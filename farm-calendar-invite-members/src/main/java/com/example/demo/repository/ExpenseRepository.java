package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByOwnerEmailOrderByDateDescIdDesc(String ownerEmail);
    Optional<Expense> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
