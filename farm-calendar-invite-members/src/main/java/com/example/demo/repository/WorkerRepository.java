package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Worker;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    List<Worker> findAllByOwnerEmailOrderByNameAsc(String ownerEmail);
    Optional<Worker> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
