package com.example.demo.conotroller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.entity.AppSetting;
import com.example.demo.entity.Crop;
import com.example.demo.entity.Schedule;
import com.example.demo.model.CalendarDay;
import com.example.demo.repository.AppSettingRepository;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.WorkerRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ScheduleService;
import com.example.demo.util.ScheduleSearch;

@Controller
public class CalendarController {

    private final ScheduleService scheduleService;
    private final CropRepository cropRepository;
    private final FieldRepository fieldRepository;
    private final WorkerRepository workerRepository;
    private final AppSettingRepository settingRepository;
    private final CurrentUserService currentUser;

    public CalendarController(ScheduleService scheduleService, CropRepository cropRepository,
            FieldRepository fieldRepository, WorkerRepository workerRepository,
            AppSettingRepository settingRepository, CurrentUserService currentUser) {
        this.scheduleService = scheduleService;
        this.cropRepository = cropRepository;
        this.fieldRepository = fieldRepository;
        this.workerRepository = workerRepository;
        this.settingRepository = settingRepository;
        this.currentUser = currentUser;
    }

    @GetMapping({"/", "/calendar"})
    public String calendar(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String scope,
            Model model) {

        String owner = currentUser.email();
        YearMonth ym = (year == null || month == null) ? YearMonth.now() : YearMonth.of(year, month);
        model.addAttribute("year", ym.getYear());
        model.addAttribute("month", ym.getMonthValue());
        model.addAttribute("monthValue", ym.toString());

        YearMonth prev = ym.minusMonths(1);
        YearMonth next = ym.plusMonths(1);
        model.addAttribute("prevYear", prev.getYear());
        model.addAttribute("prevMonth", prev.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());

        List<CalendarDay> calendarDays = new ArrayList<>();
        LocalDate firstDay = ym.atDay(1);
        int firstWeek = firstDay.getDayOfWeek().getValue() % 7;
        YearMonth prevMonth = ym.minusMonths(1);
        int prevLastDay = prevMonth.lengthOfMonth();

        for (int i = firstWeek - 1; i >= 0; i--) {
            LocalDate date = prevMonth.atDay(prevLastDay - i);
            calendarDays.add(new CalendarDay(date.getDayOfMonth(), date.toString(), false,
                    scheduleService.findByDate(date.toString())));
        }
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            calendarDays.add(new CalendarDay(day, date.toString(), true,
                    scheduleService.findByDate(date.toString())));
        }
        int nextDay = 1;
        while (calendarDays.size() < 42) {
            LocalDate date = next.atDay(nextDay);
            calendarDays.add(new CalendarDay(nextDay, date.toString(), false,
                    scheduleService.findByDate(date.toString())));
            nextDay++;
        }
        model.addAttribute("calendarDays", calendarDays);

        LocalDate today = LocalDate.now();
        List<Schedule> todaySchedules = scheduleService.findByDate(today.toString());
        long completedCount = todaySchedules.stream().filter(s -> "完了".equals(s.getStatus())).count();

        List<Crop> crops = cropRepository.findAllByOwnerEmailOrderByIdDesc(owner);
        long activeCropCount = crops.stream().filter(c -> !"収穫済".equals(c.getStatus())).count();
        double monthlyHarvestKg = crops.stream()
                .filter(c -> c.getHarvestDate() != null && YearMonth.from(c.getHarvestDate()).equals(ym))
                .map(Crop::getExpectedHarvestKg).filter(v -> v != null && v > 0)
                .mapToDouble(Double::doubleValue).sum();
        AppSetting setting = settingRepository.findFirstByOwnerEmail(owner).orElse(new AppSetting());
        double harvestTargetKg = setting.getMonthlyHarvestTargetKg() == null ? 1000.0 : setting.getMonthlyHarvestTargetKg();
        if (harvestTargetKg <= 0) harvestTargetKg = 1000.0;
        int harvestProgress = (int) Math.min(100, Math.round((monthlyHarvestKg / harvestTargetKg) * 100));

        List<Schedule> allSchedules = scheduleService.findAll();
        String searchQuery = q.strip();
        String searchScope = "month".equals(scope) ? "month" : "all";
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("searchScope", searchScope);
        model.addAttribute("searchResults", ScheduleSearch.find(allSchedules, searchQuery,
                "month".equals(searchScope) ? ym : null));

        Set<String> workers = new LinkedHashSet<>();
        Set<String> cropNames = new LinkedHashSet<>();
        Set<String> workTypes = new LinkedHashSet<>();
        workerRepository.findAllByOwnerEmailOrderByNameAsc(owner).stream()
                .filter(worker -> !Boolean.FALSE.equals(worker.getActive()))
                .forEach(worker -> addIfPresent(workers, worker.getName()));
        allSchedules.forEach(schedule -> {
            addIfPresent(workers, schedule.getUserName());
            addIfPresent(cropNames, schedule.getCropName());
            addIfPresent(workTypes, schedule.getWorkType());
        });

        model.addAttribute("todayLabel", today.format(DateTimeFormatter.ofPattern("yyyy年M月d日（E）", Locale.JAPANESE)));
        model.addAttribute("todayShortLabel", today.format(DateTimeFormatter.ofPattern("M/d")));
        model.addAttribute("todayIso", today.toString());
        model.addAttribute("todaySchedules", todaySchedules);
        model.addAttribute("todayScheduleCount", todaySchedules.size());
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("unfinishedCount", todaySchedules.size() - completedCount);
        model.addAttribute("fieldCount", fieldRepository.countByOwnerEmail(owner));
        model.addAttribute("activeCropCount", activeCropCount);
        model.addAttribute("monthlyHarvestKg", monthlyHarvestKg);
        model.addAttribute("harvestTargetKg", harvestTargetKg);
        model.addAttribute("harvestProgress", harvestProgress);
        model.addAttribute("workers", workers);
        model.addAttribute("cropNames", cropNames);
        model.addAttribute("workTypes", workTypes);
        return "calendar";
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) values.add(value.strip());
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
            @RequestParam String date, @RequestParam String startTime,
            @RequestParam String endTime, @RequestParam String userName,
            @RequestParam String schedule,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String cropName,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String memo) {
        Schedule s = id == null ? new Schedule() : scheduleService.findById(id);
        if (s == null) return "redirect:/calendar";
        applySchedule(s, date, startTime, endTime, userName, schedule, fieldName, cropName, workType, status, memo);
        scheduleService.save(s);
        LocalDate d = LocalDate.parse(date);
        return "redirect:/calendar?year=" + d.getYear() + "&month=" + d.getMonthValue();
    }

    @GetMapping("/schedule")
    @ResponseBody
    public List<Schedule> getSchedule(@RequestParam String date) {
        return scheduleService.findByDate(date);
    }

    @GetMapping("/schedule/edit")
    @ResponseBody
    public Schedule edit(@RequestParam Long id) {
        return scheduleService.findById(id);
    }

    @PostMapping("/update")
    public String update(@RequestParam Long id, @RequestParam String date,
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam String userName, @RequestParam String schedule,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String cropName,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String memo) {
        Schedule s = scheduleService.findById(id);
        if (s == null) return "redirect:/calendar";
        applySchedule(s, date, startTime, endTime, userName, schedule, fieldName, cropName, workType, status, memo);
        scheduleService.save(s);
        LocalDate d = LocalDate.parse(date);
        return "redirect:/calendar?year=" + d.getYear() + "&month=" + d.getMonthValue();
    }

    private void applySchedule(Schedule s, String date, String startTime, String endTime,
            String userName, String schedule, String fieldName, String cropName,
            String workType, String status, String memo) {
        s.setDate(date);
        s.setStartTime(startTime);
        s.setEndTime(endTime);
        s.setUserName(userName);
        s.setSchedule(schedule);
        s.setFieldName(fieldName);
        s.setCropName(cropName);
        s.setWorkType(workType);
        s.setStatus(status == null || status.isBlank() ? "未着手" : status);
        s.setMemo(memo);
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return "ok";
    }

    @GetMapping("/calendar/day")
    @ResponseBody
    public List<Schedule> getDaySchedule(@RequestParam String date) {
        return scheduleService.findByDate(date);
    }
}
