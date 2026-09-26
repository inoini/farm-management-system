package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Farm;

public interface FarmRepository extends JpaRepository<Farm, Long> {
    Optional<Farm> findByDataKey(String dataKey);
}
