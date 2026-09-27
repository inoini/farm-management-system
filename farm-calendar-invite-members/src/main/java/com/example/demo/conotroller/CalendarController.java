package com.example.demo.conotroller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
        LocalDate calendarStart = firstDay.minusDays(firstWeek);
        LocalDate calendarEnd = calendarStart.plusDays(41);

        // 42日分を日ごとに42回問い合わせず、1回でまとめて取得する。
        // Render + 外部DB環境でトップへ戻る際の待ち時間を大きく減らす。
        List<Schedule> displayedSchedules = scheduleService.findBetween(
                calendarStart.toString(), calendarEnd.toString());
        Map<String, List<Schedule>> schedulesByDate = new HashMap<>();
        displayedSchedules.forEach(schedule ->
                schedulesByDate.computeIfAbsent(schedule.getDate(), key -> new ArrayList<>()).add(schedule));

        for (int i = 0; i < 42; i++) {
            LocalDate date = calendarStart.plusDays(i);
            calendarDays.add(new CalendarDay(
                    date.getDayOfMonth(),
                    date.toString(),
                    YearMonth.from(date).equals(ym),
                    schedulesByDate.getOrDefault(date.toString(), List.of())));
        }
        model.addAttribute("calendarDays", calendarDays);

        LocalDate today = LocalDate.now();
        List<Schedule> todaySchedules;
        if (!today.isBefore(calendarStart) && !today.isAfter(calendarEnd)) {
            todaySchedules = schedulesByDate.getOrDefault(today.toString(), List.of());
        } else {
            todaySchedules = scheduleService.findByDate(today.toString());
        }
        long completedCount = todaySchedules.stream().filter(item -> "完了".equals(item.getStatus())).count();

        String searchQuery = q.strip();
        String searchScope = "month".equals(scope) ? "month" : "all";
        List<Schedule> searchSource;
        if (searchQuery.isBlank()) {
            searchSource = List.of();
        } else if ("month".equals(searchScope)) {
            searchSource = displayedSchedules;
        } else {
            searchSource = scheduleService.findAll();
        }
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("searchScope", searchScope);
        model.addAttribute("searchResults", ScheduleSearch.find(searchSource, searchQuery,
                "month".equals(searchScope) ? ym : null));

        Set<String> workers = new LinkedHashSet<>();
        Set<String> cropNames = new LinkedHashSet<>();
        Set<String> workTypes = new LinkedHashSet<>();
        workerRepository.findAllByOwnerEmailOrderByNameAsc(owner).stream()
                .filter(worker -> !Boolean.FALSE.equals(worker.getActive()))
                .forEach(worker -> addIfPresent(workers, worker.getName()));
        displayedSchedules.forEach(schedule -> {
            addIfPresent(workers, schedule.getUserName());
            addIfPresent(cropNames, schedule.getCropName());
            addIfPresent(workTypes, schedule.getWorkType());
        });

        model.addAttribute("todayLabel", today.format(DateTimeFormatter.ofPattern("yyyy年M月d日（E）", Locale.JAPANESE)));
        model.addAttribute("todayIso", today.toString());
        model.addAttribute("unfinishedCount", todaySchedules.size() - completedCount);
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
