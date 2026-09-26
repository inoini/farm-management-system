package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findAllByOwnerEmailOrderByItemNameAsc(String ownerEmail);
    Optional<Stock> findByIdAndOwnerEmail(Long id, String ownerEmail);
    Optional<Stock> findByOwnerEmailAndItemName(String ownerEmail, String itemName);
    List<Stock> findByOwnerEmailAndQuantityLessThanEqual(String ownerEmail, Integer quantity);
}
