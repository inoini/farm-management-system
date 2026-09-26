package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Schedule;
import com.example.demo.repository.ScheduleRepository;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final StockService stockService;
    private final CurrentUserService currentUser;

    public ScheduleService(ScheduleRepository scheduleRepository, StockService stockService,
            CurrentUserService currentUser) {
        this.scheduleRepository = scheduleRepository;
        this.stockService = stockService;
        this.currentUser = currentUser;
    }

    @Transactional
    public Schedule save(Schedule schedule) {
        String owner = currentUser.email();
        boolean isNew = schedule.getId() == null;
        if (!isNew) {
            Schedule stored = scheduleRepository.findByIdAndOwnerEmail(schedule.getId(), owner).orElse(null);
            if (stored == null) {
                throw new IllegalArgumentException("対象の予定が見つかりません。");
            }
            schedule.setOwnerEmail(owner);
        } else {
            schedule.setOwnerEmail(owner);
        }

        Schedule saved = scheduleRepository.save(schedule);
        if (isNew && schedule.getItemName() != null && !schedule.getItemName().isBlank()
                && schedule.getUseQuantity() != null && schedule.getUseQuantity() > 0) {
            stockService.useStock(schedule.getItemName(), schedule.getUseQuantity());
        }
        return saved;
    }

    public List<Schedule> findAll() {
        return scheduleRepository.findAllByOwnerEmail(currentUser.email());
    }

    public List<Schedule> findByDate(String date) {
        return scheduleRepository.findByOwnerEmailAndDateOrderByStartTimeAsc(currentUser.email(), date);
    }

    public List<Schedule> findByDateAndUserName(String date, String userName) {
        return scheduleRepository.findByOwnerEmailAndDateAndUserNameOrderByStartTimeAsc(
                currentUser.email(), date, userName);
    }

    public Schedule findById(Long id) {
        return scheduleRepository.findByIdAndOwnerEmail(id, currentUser.email()).orElse(null);
    }

    @Transactional
    public void delete(Long id) {
        scheduleRepository.findByIdAndOwnerEmail(id, currentUser.email())
                .ifPresent(scheduleRepository::delete);
    }

    public Schedule update(Schedule schedule) {
        return save(schedule);
    }
}
