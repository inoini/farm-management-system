package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.FarmInvite;

public interface FarmInviteRepository extends JpaRepository<FarmInvite, Long> {
    Optional<FarmInvite> findByCodeHashAndRevokedFalse(String codeHash);
    List<FarmInvite> findAllByFarmIdAndRevokedFalseOrderByCreatedAtDesc(Long farmId);
    List<FarmInvite> findAllByFarmId(Long farmId);
}
