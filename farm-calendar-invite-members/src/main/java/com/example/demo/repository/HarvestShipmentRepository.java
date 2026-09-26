package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.HarvestShipment;

public interface HarvestShipmentRepository extends JpaRepository<HarvestShipment, Long> {
    List<HarvestShipment> findAllByOwnerEmailOrderByWorkDateDescIdDesc(String ownerEmail);
    Optional<HarvestShipment> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
