package com.example.demo.service;

import com.example.demo.entity.Crop;
import com.example.demo.entity.Expense;
import com.example.demo.entity.Field;
import com.example.demo.entity.Sales;
import com.example.demo.entity.Schedule;
import com.example.demo.entity.Stock;
import com.example.demo.util.FieldLabels;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Exports only records already scoped to the current farm by the caller.
 * Does not change original data, create new database tables or modify reports.
 */
@Service
public class FarmExcelExportService {

    private record CropField(String crop, Long fieldId) {}

    public byte[] management(int year, List<Sales> allSales, List<Expense> allExpenses,
                             List<Field> fields) throws IOException {
        List<Sales> sales = allSales.stream()
                .filter(s -> s.getDate() != null && s.getDate().getYear() == year).toList();
        List<Expense> expenses = allExpenses.stream()
                .filter(e -> e.getDate() != null && e.getDate().getYear() == year).toList();
        Map<Long, String> fieldNames = FieldLabels.from(fields);
        Map<Long, double[]> fieldTotals = new LinkedHashMap<>();
        for (Field f : fields) fieldTotals.put(f.getId(), new double[2]);
        Map<CropField, double[]> cropTotals = new LinkedHashMap<>();

        List<Object[]> saleRows = new ArrayList<>();
        for (Sales sale : sales) {
            double amount = finite(sale.getAmount());
            Long fieldId = sale.getFieldId();
            fieldTotals.computeIfAbsent(fieldId, ignored -> new double[2])[0] += amount;
            String crop = trim(sale.getCrop());
            if (!crop.isEmpty() || fieldId != null) {
                cropTotals.computeIfAbsent(new CropField(crop, fieldId), ignored -> new double[2])[0] += amount;
            }
            saleRows.add(new Object[]{date(sale.getDate()), crop, FieldLabels.label(fieldNames, fieldId),
                    amount, number(sale.getTotalWeightKg()), trim(sale.getMemo())});
        }

        List<Object[]> expenseRows = new ArrayList<>();
        for (Expense expense : expenses) {
            double amount = finite(expense.getAmount());
            Long fieldId = expense.getFieldId();
            fieldTotals.computeIfAbsent(fieldId, ignored -> new double[2])[1] += amount;
            String crop = trim(expense.getCrop());
            if (!crop.isEmpty() || fieldId != null) {
                cropTotals.computeIfAbsent(new CropField(crop, fieldId), ignored -> new double[2])[1] += amount;
            }
            expenseRows.add(new Object[]{date(expense.getDate()), trim(expense.getCategory()), crop,
                    FieldLabels.label(fieldNames, fieldId), amount, trim(expense.getMemo())});
        }

        List<Object[]> fieldRows = new ArrayList<>();
        fieldTotals.forEach((id, total) -> fieldRows.add(new Object[]{
                FieldLabels.label(fieldNames, id), round(total[0]), round(total[1]), round(total[0] - total[1])
        }));
        List<Object[]> cropRows = new ArrayList<>();
        cropTotals.forEach((key, total) -> cropRows.add(new Object[]{
                key.crop().isEmpty() ? "作物未設定" : key.crop(), FieldLabels.label(fieldNames, key.fieldId()),
                round(total[0]), round(total[1]), round(total[0] - total[1])
        }));

        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            sheet(book, "圃場別収支", new String[]{"圃場", "売上（円）", "経費（円）", "利益（円）"}, fieldRows);
            sheet(book, "作物別収支", new String[]{"作物", "圃場", "売上（円）", "経費（円）", "利益（円）"}, cropRows);
            sheet(book, "売上明細", new String[]{"日付", "作物", "圃場", "売上（円）", "総重量（kg）", "メモ"}, saleRows);
            sheet(book, "経費明細", new String[]{"日付", "経費区分", "作物", "圃場", "経費（円）", "メモ"}, expenseRows);
            book.write(out);
            return out.toByteArray();
        }
    }

    public byte[] workHistory(int year, List<Schedule> work) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        String prefix = year + "-";
        for (Schedule s : work) {
            if (s.getDate() == null || !s.getDate().startsWith(prefix)) continue;
            rows.add(new Object[]{trim(s.getDate()), trim(s.getStartTime()), trim(s.getEndTime()),
                    trim(s.getUserName()), trim(s.getFieldName()), trim(s.getCropName()),
                    trim(s.getWorkType()), trim(s.getSchedule()), trim(s.getStatus()),
                    trim(s.getItemName()), s.getUseQuantity(), trim(s.getMemo())});
        }
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            sheet(book, "作業履歴", new String[]{"作業日", "開始", "終了", "担当者", "圃場", "作物",
                    "作業種類", "作業内容", "進捗", "使用資材", "使用数量", "メモ"}, rows);
            book.write(out);
            return out.toByteArray();
        }
    }

    public byte[] inventory(List<Stock> stocks) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (Stock s : stocks) {
            rows.add(new Object[]{trim(s.getItemName()), trim(s.getCategory()), s.getQuantity(),
                    trim(s.getUnit()), s.getMinimumStock(), trim(s.getLocation()),
                    trim(s.getTargetCrop()), trim(s.getDilutionRate()), s.getMaxUsageCount(),
                    s.getUsedCount(), s.getPreHarvestDays(), trim(s.getFertilizerComposition()),
                    date(s.getLastUsedDate()), trim(s.getMemo())});
        }
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            sheet(book, "在庫一覧", new String[]{"資材名", "分類", "数量", "単位", "最低在庫", "保管場所",
                    "対象作物", "登録した希釈倍率", "登録した最大使用回数", "入力済使用回数", "収穫前日数",
                    "肥料成分", "最終使用日", "メモ"}, rows);
            book.write(out);
            return out.toByteArray();
        }
    }

    public byte[] crops(List<Crop> crops) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (Crop c : crops) {
            rows.add(new Object[]{trim(c.getCropName()), trim(c.getVariety()), trim(c.getFieldName()),
                    date(c.getPlantingDate()), date(c.getHarvestDate()),
                    c.getArea(), c.getExpectedHarvestKg(), trim(c.getStatus()),
                    date(c.getWeatherAdjustedHarvestDate()), trim(c.getTopdressingGuide())});
        }
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            sheet(book, "作物一覧", new String[]{"作物名", "品種", "圃場", "植付日", "収穫予定日",
                    "面積（a）", "収穫予定量（kg）", "状態", "天候調整収穫日", "追肥の目安"}, rows);
            book.write(out);
            return out.toByteArray();
        }
    }

    public byte[] fields(List<Field> fields) throws IOException {
        Map<Long, String> names = FieldLabels.from(fields);
        List<Object[]> rows = new ArrayList<>();
        for (Field f : fields) {
            rows.add(new Object[]{FieldLabels.label(names, f.getId()), trim(f.getLocation()),
                    f.getArea(), trim(f.getCropName()), trim(f.getMemo())});
        }
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            sheet(book, "圃場一覧", new String[]{"圃場名", "住所", "面積（a）", "栽培作物", "メモ"}, rows);
            book.write(out);
            return out.toByteArray();
        }
    }

    private static void sheet(XSSFWorkbook book, String name, String[] header, List<Object[]> data) {
        Sheet sheet = book.createSheet(name);
        Row top = sheet.createRow(0);
        Font font = book.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle headerStyle = book.createCellStyle();
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CellStyle numberStyle = book.createCellStyle();
        numberStyle.setDataFormat(book.createDataFormat().getFormat("#,##0.###;[Red]-#,##0.###"));
        for (int i = 0; i < header.length; i++) {
            Cell cell = top.createCell(i);
            cell.setCellValue(header[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 22 * 256);
        }
        for (Object[] values : data) {
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value == null) continue;
                Cell cell = row.createCell(i);
                if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                    cell.setCellStyle(numberStyle);
                } else {
                    // Explicit string type: a user's "=..." text is never an Excel formula.
                    cell.setCellValue(value.toString());
                }
            }
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                0, Math.max(0, sheet.getLastRowNum()), 0, header.length - 1));
    }

    private static String trim(String input) {
        return input == null ? "" : input.strip();
    }

    private static String date(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private static Double number(java.math.BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static double finite(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
