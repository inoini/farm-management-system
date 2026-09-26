package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByOwnerEmailAndDateOrderByStartTimeAsc(String ownerEmail, String date);
    List<Schedule> findByOwnerEmailAndDateAndUserNameOrderByStartTimeAsc(String ownerEmail, String date, String userName);
    List<Schedule> findAllByOwnerEmailOrderByDateDescStartTimeDesc(String ownerEmail);
    List<Schedule> findAllByOwnerEmail(String ownerEmail);
    Optional<Schedule> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
