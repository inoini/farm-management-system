package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Crop;

public interface CropRepository extends JpaRepository<Crop, Long> {
    List<Crop> findAllByOwnerEmailOrderByIdDesc(String ownerEmail);
    Optional<Crop> findByIdAndOwnerEmail(Long id, String ownerEmail);
    boolean existsByIdAndOwnerEmail(Long id, String ownerEmail);
    long countByOwnerEmail(String ownerEmail);
    List<Crop> findByOwnerEmailAndHarvestDateGreaterThanEqualOrderByHarvestDateAsc(String ownerEmail, LocalDate date);
}
