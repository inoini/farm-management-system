package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    List<UserAccount> findAllByEmailIgnoreCaseOrderByCreatedAtAsc(String email);
    Optional<UserAccount> findFirstByEmailIgnoreCaseAndFarmRoleIgnoreCase(String email, String farmRole);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndFarmRoleIgnoreCase(String email, String farmRole);
    boolean existsByUsernameIgnoreCase(String username);
    List<UserAccount> findAllByFarmIdOrderByCreatedAtAsc(Long farmId);
}
