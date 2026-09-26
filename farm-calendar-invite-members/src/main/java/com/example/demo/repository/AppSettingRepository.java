package com.example.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findFirstByOwnerEmail(String ownerEmail);
}
