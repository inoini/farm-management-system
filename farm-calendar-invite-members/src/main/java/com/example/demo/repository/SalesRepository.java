package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Sales;

public interface SalesRepository extends JpaRepository<Sales, Long> {
    List<Sales> findAllByOwnerEmailOrderByDateDescIdDesc(String ownerEmail);
    Optional<Sales> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
