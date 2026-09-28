package com.example.demo.conotroller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.AppSetting;
import com.example.demo.entity.Expense;
import com.example.demo.entity.HarvestShipment;
import com.example.demo.entity.Sales;
import com.example.demo.entity.Schedule;
import com.example.demo.entity.Worker;
import com.example.demo.repository.AppSettingRepository;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.HarvestShipmentRepository;
import com.example.demo.repository.SalesRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.WorkerRepository;
import com.example.demo.service.CurrentUserService;

@Controller
public class OperationsController {

    private final ScheduleRepository scheduleRepository;
    private final WorkerRepository workerRepository;
    private final HarvestShipmentRepository harvestRepository;
    private final AppSettingRepository settingRepository;
    private final CropRepository cropRepository;
    private final FieldRepository fieldRepository;
    private final SalesRepository salesRepository;
    private final ExpenseRepository expenseRepository;
    private final CurrentUserService currentUser;

    public OperationsController(ScheduleRepository scheduleRepository, WorkerRepository workerRepository,
            HarvestShipmentRepository harvestRepository, AppSettingRepository settingRepository,
            CropRepository cropRepository, FieldRepository fieldRepository,
            SalesRepository salesRepository, ExpenseRepository expenseRepository,
            CurrentUserService currentUser) {
        this.scheduleRepository = scheduleRepository;
        this.workerRepository = workerRepository;
        this.harvestRepository = harvestRepository;
        this.settingRepository = settingRepository;
        this.cropRepository = cropRepository;
        this.fieldRepository = fieldRepository;
        this.salesRepository = salesRepository;
        this.expenseRepository = expenseRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/work-history")
    public String workHistory(Model model) {
        List<Schedule> schedules = scheduleRepository.findAllByOwnerEmailOrderByDateDescStartTimeDesc(currentUser.email());
        model.addAttribute("schedules", schedules);
        model.addAttribute("totalCount", schedules.size());
        model.addAttribute("completedCount", schedules.stream().filter(s -> "完了".equals(s.getStatus())).count());
        return "operations/work-history";
    }

    @PostMapping("/work-history/status/{id}")
    public String updateWorkStatus(@PathVariable Long id, @RequestParam String status,
            RedirectAttributes redirect) {
        Schedule work = scheduleRepository.findByIdAndOwnerEmail(id, currentUser.email()).orElse(null);
        if (work == null) redirect.addFlashAttribute("error", "対象の作業が見つかりません。");
        else if (!validWorkStatus(status)) redirect.addFlashAttribute("error", "進捗状態を選び直してください。");
        else {
            work.setStatus(status);
            scheduleRepository.save(work);
            redirect.addFlashAttribute("message", "進捗を更新しました。");
        }
        return "redirect:/work-history";
    }

    @GetMapping("/work-history/edit/{id}")
    public String editWork(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Schedule work = scheduleRepository.findByIdAndOwnerEmail(id, currentUser.email()).orElse(null);
        if (work == null) {
            redirect.addFlashAttribute("error", "対象の作業が見つかりません。");
            return "redirect:/work-history";
        }
        model.addAttribute("editWork", work);
        return workHistory(model);
    }

    @PostMapping("/work-history/update/{id}")
    public String updateWork(@PathVariable Long id, @RequestParam String date,
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam String userName, @RequestParam String schedule,
            @RequestParam(value = "fieldName", defaultValue = "") String fieldName,
            @RequestParam(value = "cropName", defaultValue = "") String cropName,
            @RequestParam(value = "workType", defaultValue = "") String workType,
            @RequestParam String status,
            @RequestParam(value = "memo", defaultValue = "") String memo,
            Model model, RedirectAttributes redirect) {
        Schedule work = scheduleRepository.findByIdAndOwnerEmail(id, currentUser.email()).orElse(null);
        if (work == null) {
            redirect.addFlashAttribute("error", "対象の作業が見つかりません。");
            return "redirect:/work-history";
        }
        Schedule input = new Schedule();
        input.setId(id); input.setDate(date); input.setStartTime(startTime); input.setEndTime(endTime);
        input.setUserName(userName); input.setSchedule(schedule); input.setFieldName(fieldName);
        input.setCropName(cropName); input.setWorkType(workType); input.setStatus(status); input.setMemo(memo);
        String error = null;
        try {
            LocalDate.parse(date);
            if (!LocalTime.parse(endTime).isAfter(LocalTime.parse(startTime))) error = "終了時間は開始時間より後にしてください。";
        } catch (DateTimeParseException ex) {
            error = "日付・開始時間・終了時間を正しく入力してください。";
        }
        if (userName.isBlank() || schedule.isBlank() || !validWorkStatus(status)) error = "担当者・作業内容・進捗状態を入力してください。";
        if (userName.length() > 50 || schedule.length() > 500 || fieldName.length() > 100
                || cropName.length() > 100 || workType.length() > 100 || memo.length() > 1000) {
            error = "入力文字数が上限を超えています。";
        }
        if (error != null) {
            model.addAttribute("editWork", input);
            model.addAttribute("error", error);
            return workHistory(model);
        }
        work.setDate(date); work.setStartTime(startTime); work.setEndTime(endTime);
        work.setUserName(userName); work.setSchedule(schedule); work.setFieldName(fieldName);
        work.setCropName(cropName); work.setWorkType(workType); work.setStatus(status); work.setMemo(memo);
        scheduleRepository.save(work);
        redirect.addFlashAttribute("message", "作業内容を更新しました。");
        return "redirect:/work-history";
    }

    @PostMapping("/work-history/delete/{id}")
    public String deleteWork(@PathVariable Long id, RedirectAttributes redirect) {
        var work = scheduleRepository.findByIdAndOwnerEmail(id, currentUser.email());
        if (work.isEmpty()) redirect.addFlashAttribute("error", "対象の作業が見つかりません。");
        else {
            scheduleRepository.delete(work.get());
            redirect.addFlashAttribute("message", "作業を消去しました。");
        }
        return "redirect:/work-history";
    }

    private boolean validWorkStatus(String status) {
        return "未着手".equals(status) || "進行中".equals(status) || "完了".equals(status);
    }

    @GetMapping("/workers")
    public String workers(Model model) {
        model.addAttribute("workers", workerRepository.findAllByOwnerEmailOrderByNameAsc(currentUser.email()));
        model.addAttribute("worker", new Worker());
        return "operations/workers";
    }

    @GetMapping("/workers/edit/{id}")
    public String editWorker(@PathVariable Long id, Model model) {
        String owner = currentUser.email();
        model.addAttribute("workers", workerRepository.findAllByOwnerEmailOrderByNameAsc(owner));
        model.addAttribute("worker", workerRepository.findByIdAndOwnerEmail(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "operations/workers";
    }

    @PostMapping("/workers/save")
    public String saveWorker(@ModelAttribute Worker worker, @RequestParam(defaultValue = "false") boolean active) {
        String owner = currentUser.email();
        if (worker.getId() != null && workerRepository.findByIdAndOwnerEmail(worker.getId(), owner).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        worker.setOwnerEmail(owner);
        worker.setActive(active);
        workerRepository.save(worker);
        return "redirect:/workers";
    }

    @PostMapping("/workers/delete/{id}")
    public String deleteWorker(@PathVariable Long id) {
        workerRepository.findByIdAndOwnerEmail(id, currentUser.email()).ifPresent(workerRepository::delete);
        return "redirect:/workers";
    }

    @GetMapping("/harvests")
    public String harvests(Model model) {
        model.addAttribute("records", harvestRepository.findAllByOwnerEmailOrderByWorkDateDescIdDesc(currentUser.email()));
        model.addAttribute("record", new HarvestShipment());
        return "operations/harvests";
    }

    @GetMapping("/harvests/edit/{id}")
    public String editHarvest(@PathVariable Long id, Model model) {
        String owner = currentUser.email();
        model.addAttribute("records", harvestRepository.findAllByOwnerEmailOrderByWorkDateDescIdDesc(owner));
        model.addAttribute("record", harvestRepository.findByIdAndOwnerEmail(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "operations/harvests";
    }

    @PostMapping("/harvests/save")
    public String saveHarvest(@ModelAttribute HarvestShipment record) {
        String owner = currentUser.email();
        if (record.getId() != null && harvestRepository.findByIdAndOwnerEmail(record.getId(), owner).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        record.setOwnerEmail(owner);
        if (record.getWorkDate() == null) record.setWorkDate(LocalDate.now());
        if (record.getStatus() == null || record.getStatus().isBlank()) record.setStatus("収穫済");
        harvestRepository.save(record);
        return "redirect:/harvests";
    }

    @PostMapping("/harvests/delete/{id}")
    public String deleteHarvest(@PathVariable Long id) {
        harvestRepository.findByIdAndOwnerEmail(id, currentUser.email()).ifPresent(harvestRepository::delete);
        return "redirect:/harvests";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        String owner = currentUser.email();
        List<Schedule> schedules = scheduleRepository.findAllByOwnerEmail(owner);
        List<HarvestShipment> harvests = harvestRepository.findAllByOwnerEmailOrderByWorkDateDescIdDesc(owner);
        List<Sales> salesItems = salesRepository.findAllByOwnerEmailOrderByDateDescIdDesc(owner);
        List<Expense> expenseItems = expenseRepository.findAllByOwnerEmailOrderByDateDescIdDesc(owner);

        long completed = schedules.stream().filter(s -> "完了".equals(s.getStatus())).count();
        double harvestedKg = harvests.stream().map(HarvestShipment::getHarvestKg).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double shippedKg = harvests.stream().map(HarvestShipment::getShippedKg).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double sales = salesItems.stream().map(Sales::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double expenses = expenseItems.stream().map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();

        YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Tokyo"));
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (int offset = 11; offset >= 0; offset--) {
            YearMonth month = currentMonth.minusMonths(offset);
            double monthHarvest = harvests.stream()
                    .filter(item -> item.getWorkDate() != null && YearMonth.from(item.getWorkDate()).equals(month))
                    .map(HarvestShipment::getHarvestKg).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
            double monthSales = salesItems.stream()
                    .filter(item -> item.getDate() != null && YearMonth.from(item.getDate()).equals(month))
                    .map(Sales::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
            double monthExpenses = expenseItems.stream()
                    .filter(item -> item.getDate() != null && YearMonth.from(item.getDate()).equals(month))
                    .map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", month.getYear() + "年" + month.getMonthValue() + "月");
            point.put("harvestKg", Math.round(monthHarvest * 10.0) / 10.0);
            point.put("sales", Math.round(monthSales));
            point.put("expenses", Math.round(monthExpenses));
            point.put("profit", Math.round(monthSales - monthExpenses));
            monthlyTrend.add(point);
        }

        List<String> cropNames = harvests.stream().map(HarvestShipment::getCropName)
                .filter(name -> name != null && !name.isBlank()).map(String::trim).distinct().sorted().toList();
        List<Map<String, Object>> cropTrends = new ArrayList<>();
        for (String cropName : cropNames) {
            List<Map<String, Object>> points = new ArrayList<>();
            for (int offset = 11; offset >= 0; offset--) {
                YearMonth month = currentMonth.minusMonths(offset);
                double monthCropHarvest = harvests.stream()
                        .filter(item -> item.getWorkDate() != null && YearMonth.from(item.getWorkDate()).equals(month))
                        .filter(item -> item.getCropName() != null && cropName.equals(item.getCropName().trim()))
                        .map(HarvestShipment::getHarvestKg).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
                double monthCropShipped = harvests.stream()
                        .filter(item -> item.getWorkDate() != null && YearMonth.from(item.getWorkDate()).equals(month))
                        .filter(item -> item.getCropName() != null && cropName.equals(item.getCropName().trim()))
                        .map(HarvestShipment::getShippedKg).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("month", month.getYear() + "年" + month.getMonthValue() + "月");
                point.put("harvestKg", Math.round(monthCropHarvest * 10.0) / 10.0);
                point.put("shippedKg", Math.round(monthCropShipped * 10.0) / 10.0);
                points.add(point);
            }
            Map<String, Object> cropTrend = new LinkedHashMap<>();
            cropTrend.put("cropName", cropName);
            cropTrend.put("points", points);
            cropTrends.add(cropTrend);
        }

        model.addAttribute("scheduleCount", schedules.size());
        model.addAttribute("completedCount", completed);
        model.addAttribute("completionRate", schedules.isEmpty() ? 0 : Math.round(completed * 100.0 / schedules.size()));
        model.addAttribute("cropCount", cropRepository.countByOwnerEmail(owner));
        model.addAttribute("fieldCount", fieldRepository.countByOwnerEmail(owner));
        model.addAttribute("harvestedKg", harvestedKg);
        model.addAttribute("shippedKg", shippedKg);
        model.addAttribute("salesTotal", sales);
        model.addAttribute("expenseTotal", expenses);
        model.addAttribute("profitTotal", sales - expenses);
        model.addAttribute("monthlyTrend", monthlyTrend);
        model.addAttribute("cropTrends", cropTrends);
        return "operations/reports";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        AppSetting setting = settingRepository.findFirstByOwnerEmail(currentUser.email()).orElse(new AppSetting());
        setting.setUiTheme(validTheme(setting.getUiTheme()));
        setting.setLayoutMode(validLayout(setting.getLayoutMode()));
        model.addAttribute("setting", setting);
        return "operations/settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@ModelAttribute AppSetting input,
            @RequestParam(defaultValue = "false") boolean notificationsEnabled) {
        String owner = currentUser.email();
        AppSetting setting = settingRepository.findFirstByOwnerEmail(owner).orElse(new AppSetting());
        setting.setOwnerEmail(owner);
        setting.setFarmName(input.getFarmName());
        setting.setMonthlyHarvestTargetKg(input.getMonthlyHarvestTargetKg());
        setting.setNotificationsEnabled(notificationsEnabled);
        setting.setUiTheme(validTheme(input.getUiTheme()));
        String savedLayoutMode = validLayout(input.getLayoutMode());
        setting.setLayoutMode(savedLayoutMode);
        settingRepository.save(setting);

        // レイアウト2は保存した直後に作業カレンダーへ切り替えて、
        // 左メニューなし・全幅カレンダーの表示をその場で反映する。
        if ("calendar2".equals(savedLayoutMode)) {
            return "redirect:/calendar?layoutUpdated=1";
        }

        return "redirect:/settings?saved";
    }

    private String validTheme(String value) {
        return switch (value == null ? "" : value) {
            case "blue", "earth", "discord" -> value;
            default -> "green";
        };
    }

    private String validLayout(String value) {
        return switch (value == null ? "" : value) {
            case "compact", "wide", "sidebar", "calendar2" -> value;
            default -> "sidebar";
        };
    }
}
