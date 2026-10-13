package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.FarmPhoto;

public interface FarmPhotoRepository extends JpaRepository<FarmPhoto, Long> {
    Optional<FarmPhoto> findByIdAndOwnerEmail(Long id, String ownerEmail);
    List<FarmPhoto> findByOwnerEmailAndKindAndReferenceIdOrderByCreatedAtDesc(
            String ownerEmail, String kind, Long referenceId);
    List<FarmPhoto> findByOwnerEmailAndKindAndPhotoDateOrderByCreatedAtDesc(
            String ownerEmail, String kind, LocalDate photoDate);
    long countByOwnerEmailAndKindAndReferenceId(String ownerEmail, String kind, Long referenceId);
    long countByOwnerEmailAndKindAndPhotoDate(String ownerEmail, String kind, LocalDate photoDate);

    @Query("select coalesce(sum(p.sizeBytes), 0) from FarmPhoto p")
    Long totalStoredBytes();

    @Query("select p.photoDate, count(p) from FarmPhoto p "
            + "where p.ownerEmail = :owner and p.photoDate between :start and :end "
            + "group by p.photoDate")
    List<Object[]> countByDateBetween(@Param("owner") String owner,
            @Param("start") LocalDate start, @Param("end") LocalDate end);
}
