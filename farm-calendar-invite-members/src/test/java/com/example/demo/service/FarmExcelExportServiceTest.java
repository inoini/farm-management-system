package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import com.example.demo.entity.Expense;
import com.example.demo.entity.Field;
import com.example.demo.entity.Sales;
import com.example.demo.entity.Schedule;
import com.example.demo.entity.Stock;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class FarmExcelExportServiceTest {
    private final FarmExcelExportService service = new FarmExcelExportService();

    @Test
    void financeExportKeepsSameNamedFieldsSeparateAndFiltersYear() throws Exception {
        Field a = new Field(); a.setId(7L); a.setName("第一圃場");
        Field b = new Field(); b.setId(8L); b.setName("第一圃場");
        Sales one = sale(2026, 7L, 100);
        one.setMemo("=HYPERLINK(\"bad\")");
        Sales two = sale(2026, 7L, 50);
        Sales three = sale(2026, 8L, 200);
        Sales old = sale(2025, 7L, 999);
        Expense expense = new Expense(); expense.setDate(LocalDate.of(2026, 3, 1));
        expense.setFieldId(7L); expense.setCrop("さつまいも"); expense.setAmount(30.0);
        byte[] bytes = service.management(2026, List.of(one, two, three, old),
                List.of(expense), List.of(a, b));
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(4, book.getNumberOfSheets());
            var plot = book.getSheet("圃場別収支");
            assertEquals("第一圃場（ID:7）", plot.getRow(1).getCell(0).getStringCellValue());
            assertEquals(150.0, plot.getRow(1).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(30.0, plot.getRow(1).getCell(2).getNumericCellValue(), 0.001);
            assertEquals(120.0, plot.getRow(1).getCell(3).getNumericCellValue(), 0.001);
            assertEquals("第一圃場（ID:8）", plot.getRow(2).getCell(0).getStringCellValue());
            assertEquals(200.0, plot.getRow(2).getCell(1).getNumericCellValue(), 0.001);
            var sales = book.getSheet("売上明細");
            assertEquals(3, sales.getLastRowNum());
            assertEquals(CellType.STRING, sales.getRow(1).getCell(5).getCellType());
            assertTrue(sales.getRow(1).getCell(5).getStringCellValue().startsWith("="));
        }
    }

    @Test
    void workHistoryIsYearScopedAndExportDoesNotMutateData() throws Exception {
        Schedule current = new Schedule();
        current.setDate("2026-04-01"); current.setSchedule("草取り"); current.setStatus("完了");
        Schedule past = new Schedule();
        past.setDate("2025-04-01"); past.setSchedule("播種");
        byte[] bytes = service.workHistory(2026, List.of(current, past));
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(1, book.getSheet("作業履歴").getLastRowNum());
            assertEquals("草取り", book.getSheet("作業履歴").getRow(1).getCell(7).getStringCellValue());
        }
        assertEquals("草取り", current.getSchedule());
    }

    @Test
    void stockExportIncludesPesticideFields() throws Exception {
        Stock item = new Stock();
        item.setItemName("テスト薬剤"); item.setCategory("農薬");
        item.setDilutionRate("1000倍"); item.setMaxUsageCount(3);
        byte[] bytes = service.inventory(List.of(item));
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = book.getSheet("在庫一覧");
            assertEquals("テスト薬剤", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("1000倍", sheet.getRow(1).getCell(7).getStringCellValue());
            assertEquals(3.0, sheet.getRow(1).getCell(8).getNumericCellValue(), 0.001);
        }
    }

    private static Sales sale(int year, Long fieldId, double amount) {
        Sales s = new Sales();
        s.setDate(LocalDate.of(year, 2, 1));
        s.setCrop("さつまいも");
        s.setFieldId(fieldId);
        s.setAmount(amount);
        return s;
    }
}
