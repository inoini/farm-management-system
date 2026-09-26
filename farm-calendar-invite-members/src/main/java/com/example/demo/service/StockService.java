package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Stock;
import com.example.demo.repository.StockRepository;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final CurrentUserService currentUser;

    public StockService(StockRepository stockRepository, CurrentUserService currentUser) {
        this.stockRepository = stockRepository;
        this.currentUser = currentUser;
    }

    public List<Stock> findAll() {
        return stockRepository.findAllByOwnerEmailOrderByItemNameAsc(currentUser.email());
    }

    public Stock findById(Long id) {
        return stockRepository.findByIdAndOwnerEmail(id, currentUser.email()).orElse(null);
    }

    @Transactional
    public void save(Stock stock) {
        String owner = currentUser.email();
        if (stock.getId() != null && stockRepository.findByIdAndOwnerEmail(stock.getId(), owner).isEmpty()) {
            throw new IllegalArgumentException("対象の在庫が見つかりません。");
        }
        stock.setOwnerEmail(owner);
        stockRepository.save(stock);
    }

    @Transactional
    public void delete(Long id) {
        stockRepository.findByIdAndOwnerEmail(id, currentUser.email()).ifPresent(stockRepository::delete);
    }

    public Stock findByItemName(String itemName) {
        return stockRepository.findByOwnerEmailAndItemName(currentUser.email(), itemName).orElse(null);
    }

    @Transactional
    public void useStock(String itemName, int amount) {
        Stock stock = findByItemName(itemName);
        if (stock == null) return;
        int remain = (stock.getQuantity() == null ? 0 : stock.getQuantity()) - amount;
        stock.setQuantity(Math.max(0, remain));
        stockRepository.save(stock);
    }

    public List<Stock> getLowStocks() {
        return findAll().stream()
                .filter(stock -> stock.getQuantity() != null && stock.getMinimumStock() != null
                        && stock.getQuantity() <= stock.getMinimumStock())
                .toList();
    }

    public List<Stock> getOverStocks() {
        return findAll().stream()
                .filter(stock -> stock.getQuantity() != null && stock.getMinimumStock() != null
                        && stock.getQuantity() >= stock.getMinimumStock() * 3)
                .toList();
    }
}
