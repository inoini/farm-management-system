package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Field;

public interface FieldRepository extends JpaRepository<Field, Long> {
    List<Field> findAllByOwnerEmail(String ownerEmail);
    Optional<Field> findByIdAndOwnerEmail(Long id, String ownerEmail);
    boolean existsByIdAndOwnerEmail(Long id, String ownerEmail);
    long countByOwnerEmail(String ownerEmail);
}
