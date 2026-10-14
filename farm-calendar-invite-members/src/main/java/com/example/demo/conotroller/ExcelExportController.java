package com.example.demo.conotroller;

import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.SalesRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.FarmExcelExportService;
import com.example.demo.service.StockService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** All export URLs are login-protected by the existing SecurityConfig. */
@Controller
public class ExcelExportController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final FarmExcelExportService excel;
    private final SalesRepository sales;
    private final ExpenseRepository expenses;
    private final FieldRepository fields;
    private final ScheduleRepository schedules;
    private final StockService stock;
    private final CurrentUserService user;

    public ExcelExportController(FarmExcelExportService excel, SalesRepository sales,
            ExpenseRepository expenses, FieldRepository fields, ScheduleRepository schedules,
            StockService stock, CurrentUserService user) {
        this.excel = excel;
        this.sales = sales;
        this.expenses = expenses;
        this.fields = fields;
        this.schedules = schedules;
        this.stock = stock;
        this.user = user;
    }

    @GetMapping("/management/export.xlsx")
    public ResponseEntity<byte[]> management(@RequestParam(required = false) Integer year)
            throws IOException {
        int selected = selectedYear(year);
        String owner = user.email();
        return download("management-" + selected + ".xlsx",
                excel.management(selected,
                        sales.findAllByOwnerEmailOrderByDateDescIdDesc(owner),
                        expenses.findAllByOwnerEmailOrderByDateDescIdDesc(owner),
                        fields.findAllByOwnerEmail(owner)));
    }

    @GetMapping("/work-history/export.xlsx")
    public ResponseEntity<byte[]> workHistory(@RequestParam(required = false) Integer year)
            throws IOException {
        int selected = selectedYear(year);
        return download("work-history-" + selected + ".xlsx",
                excel.workHistory(selected,
                        schedules.findAllByOwnerEmailOrderByDateDescStartTimeDesc(user.email())));
    }

    @GetMapping("/stock/export.xlsx")
    public ResponseEntity<byte[]> inventory() throws IOException {
        return download("farm-inventory.xlsx", excel.inventory(stock.findAll()));
    }

    private int selectedYear(Integer year) {
        int selected = year == null ? LocalDate.now(ZoneId.of("Asia/Tokyo")).getYear() : year;
        if (selected < 2000 || selected > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "年を2000～2100で指定してください。");
        }
        return selected;
    }

    private static ResponseEntity<byte[]> download(String filename, byte[] contents) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .body(contents);
    }
}
