package com.example.demo.conotroller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Expense;
import com.example.demo.entity.Field;
import com.example.demo.entity.MonthlyBudget;
import com.example.demo.entity.Sales;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.MonthlyBudgetRepository;
import com.example.demo.repository.SalesRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.util.FieldLabels;

@Controller
public class ManagementController {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final SalesRepository salesRepository;
    private final ExpenseRepository expenseRepository;
    private final FieldRepository fieldRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final CurrentUserService currentUser;

    public ManagementController(SalesRepository salesRepository,
            ExpenseRepository expenseRepository,
            FieldRepository fieldRepository,
            MonthlyBudgetRepository budgetRepository,
            CurrentUserService currentUser) {
        this.salesRepository = salesRepository;
        this.expenseRepository = expenseRepository;
        this.fieldRepository = fieldRepository;
        this.budgetRepository = budgetRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/management")
    public String management(
            @RequestParam(name = "year", required = false) Integer requestedYear,
            @RequestParam(name = "month", required = false) Integer requestedMonth,
            Model model) {
        String owner = currentUser.email();
        LocalDate today = LocalDate.now(TOKYO);
        int selectedYear = validYear(requestedYear) ? requestedYear : today.getYear();
        int selectedMonth = validMonth(requestedMonth)
                ? requestedMonth
                : (selectedYear == today.getYear() ? today.getMonthValue() : 1);

        List<Sales> sales = salesRepository.findAllByOwnerEmailOrderByDateDescIdDesc(owner);
        List<Expense> expenses = expenseRepository.findAllByOwnerEmailOrderByDateDescIdDesc(owner);
        List<Field> fields = fieldRepository.findAllByOwnerEmail(owner);
        Map<Long, String> fieldLabels = FieldLabels.from(fields);
        List<MonthlyBudget> budgets = budgetRepository
                .findAllByOwnerEmailAndBudgetYearOrderByBudgetMonthAsc(owner, selectedYear);

        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("todayYear", today.getYear());
        model.addAttribute("currentMonth", selectedYear == today.getYear() ? today.getMonthValue() : 0);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("cropProfitRows", buildCropProfitRows(sales, expenses, fieldLabels));
        model.addAttribute("fieldProfitRows", buildFieldProfitRows(sales, expenses, fields, fieldLabels));
        model.addAttribute("unassignedFieldSales", roundMoney(sales.stream()
                .filter(s -> s.getFieldId() == null)
                .map(Sales::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum()));
        model.addAttribute("unassignedFieldExpenses", roundMoney(expenses.stream()
                .filter(e -> e.getFieldId() == null)
                .map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum()));
        model.addAttribute("sharedExpenseTotal", roundMoney(expenses.stream()
                .filter(e -> normalize(e.getCrop()).isEmpty() && e.getFieldId() == null)
                .map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum()));

        MonthlyBudget budgetForm = new MonthlyBudget();
        budgetForm.setBudgetYear(selectedYear);
        budgetForm.setBudgetMonth(selectedMonth);
        budgetRepository.findByOwnerEmailAndBudgetYearAndBudgetMonth(owner, selectedYear, selectedMonth)
                .ifPresent(existing -> {
                    budgetForm.setSalesBudget(existing.getSalesBudget());
                    budgetForm.setExpenseBudget(existing.getExpenseBudget());
                });
        model.addAttribute("budgetForm", budgetForm);
        model.addAttribute("budgetRows", buildBudgetRows(selectedYear, sales, expenses, budgets));

        double yearSalesActualTotal = sales.stream()
                .filter(s -> s.getDate() != null && s.getDate().getYear() == selectedYear)
                .map(Sales::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double yearExpenseActualTotal = expenses.stream()
                .filter(e -> e.getDate() != null && e.getDate().getYear() == selectedYear)
                .map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();

        boolean hasSalesBudget = budgets.stream().anyMatch(b -> b.getSalesBudget() != null);
        boolean hasExpenseBudget = budgets.stream().anyMatch(b -> b.getExpenseBudget() != null);
        boolean hasProfitBudget = budgets.stream()
                .anyMatch(b -> b.getSalesBudget() != null && b.getExpenseBudget() != null);

        double yearSalesBudgetTotal = budgets.stream().map(MonthlyBudget::getSalesBudget)
                .filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double yearExpenseBudgetTotal = budgets.stream().map(MonthlyBudget::getExpenseBudget)
                .filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double yearProfitBudgetTotal = budgets.stream()
                .filter(b -> b.getSalesBudget() != null && b.getExpenseBudget() != null)
                .mapToDouble(b -> b.getSalesBudget() - b.getExpenseBudget()).sum();

        model.addAttribute("yearSalesBudgetTotal", hasSalesBudget ? roundMoney(yearSalesBudgetTotal) : null);
        model.addAttribute("yearSalesActualTotal", roundMoney(yearSalesActualTotal));
        model.addAttribute("yearExpenseBudgetTotal", hasExpenseBudget ? roundMoney(yearExpenseBudgetTotal) : null);
        model.addAttribute("yearExpenseActualTotal", roundMoney(yearExpenseActualTotal));
        model.addAttribute("yearProfitBudgetTotal", hasProfitBudget ? roundMoney(yearProfitBudgetTotal) : null);
        model.addAttribute("yearProfitActualTotal", roundMoney(yearSalesActualTotal - yearExpenseActualTotal));

        return "management";
    }

    @PostMapping("/management/budget/save")
    public String saveBudget(@ModelAttribute MonthlyBudget input, RedirectAttributes redirectAttributes) {
        Integer year = input.getBudgetYear();
        Integer month = input.getBudgetMonth();
        if (!validYear(year) || !validMonth(month)
                || invalidMoney(input.getSalesBudget()) || invalidMoney(input.getExpenseBudget())) {
            redirectAttributes.addFlashAttribute("budgetError", "予算の年月と金額を確認してください。");
            return "redirect:/management#budget";
        }

        String owner = currentUser.email();
        MonthlyBudget budget = budgetRepository
                .findByOwnerEmailAndBudgetYearAndBudgetMonth(owner, year, month)
                .orElseGet(MonthlyBudget::new);
        budget.setOwnerEmail(owner);
        budget.setBudgetYear(year);
        budget.setBudgetMonth(month);
        // 空欄は 0 円に変換せず「未設定(null)」として保存する。
        budget.setSalesBudget(input.getSalesBudget());
        budget.setExpenseBudget(input.getExpenseBudget());
        budgetRepository.save(budget);
        redirectAttributes.addFlashAttribute("budgetSaved", year + "年" + month + "月の予算を保存しました。");
        return "redirect:/management?year=" + year + "&month=" + month + "#budget";
    }

    private record CropFieldKey(String cropName, Long fieldId) {}

    /**
     * Keep crop and field as separate grouping dimensions. Existing transactions
     * without a field remain visible under "圃場未設定" rather than being
     * guessed or assigned to an arbitrary plot.
     */
    private List<Map<String, Object>> buildCropProfitRows(
            List<Sales> sales, List<Expense> expenses, Map<Long, String> fieldLabels) {
        Map<CropFieldKey, double[]> totals = new LinkedHashMap<>();
        for (Sales sale : sales) {
            String crop = normalize(sale.getCrop());
            Long fieldId = sale.getFieldId();
            if (crop.isEmpty() && fieldId == null) continue;
            totals.computeIfAbsent(new CropFieldKey(crop, fieldId), key -> new double[2])[0]
                    += sale.getAmount() == null ? 0 : sale.getAmount();
        }
        for (Expense expense : expenses) {
            String crop = normalize(expense.getCrop());
            Long fieldId = expense.getFieldId();
            if (crop.isEmpty() && fieldId == null) continue; // shared costs
            totals.computeIfAbsent(new CropFieldKey(crop, fieldId), key -> new double[2])[1]
                    += expense.getAmount() == null ? 0 : expense.getAmount();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        totals.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<CropFieldKey, double[]> item) -> item.getKey().cropName())
                        .thenComparing(item -> FieldLabels.label(fieldLabels, item.getKey().fieldId()))
                        .thenComparing(item -> item.getKey().fieldId() == null ? -1L : item.getKey().fieldId()))
                .forEach(item -> {
                    double cropSales = item.getValue()[0];
                    double cropExpense = item.getValue()[1];
                    double cropProfit = cropSales - cropExpense;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("cropName", item.getKey().cropName().isEmpty() ? "作物未設定" : item.getKey().cropName());
                    row.put("fieldName", FieldLabels.label(fieldLabels, item.getKey().fieldId()));
                    row.put("sales", roundMoney(cropSales));
                    row.put("expenses", roundMoney(cropExpense));
                    row.put("profit", roundMoney(cropProfit));
                    row.put("margin", cropSales <= 0 ? 0 : Math.round(cropProfit * 1000.0 / cropSales) / 10.0);
                    rows.add(row);
                });
        return rows;
    }

    /** Sum every transaction assigned to a specific field, across all crops. */
    private List<Map<String, Object>> buildFieldProfitRows(
            List<Sales> sales, List<Expense> expenses, List<Field> fields, Map<Long, String> fieldLabels) {
        Map<Long, double[]> totals = new LinkedHashMap<>();
        for (Field field : fields) {
            totals.put(field.getId(), new double[2]);
        }
        for (Sales sale : sales) {
            if (sale.getFieldId() != null) {
                totals.computeIfAbsent(sale.getFieldId(), key -> new double[2])[0]
                        += sale.getAmount() == null ? 0 : sale.getAmount();
            }
        }
        for (Expense expense : expenses) {
            if (expense.getFieldId() != null) {
                totals.computeIfAbsent(expense.getFieldId(), key -> new double[2])[1]
                        += expense.getAmount() == null ? 0 : expense.getAmount();
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        totals.entrySet().stream()
                .sorted(Comparator.comparing(item -> FieldLabels.label(fieldLabels, item.getKey())))
                .forEach(item -> {
                    double fieldSales = item.getValue()[0];
                    double fieldExpense = item.getValue()[1];
                    double fieldProfit = fieldSales - fieldExpense;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("fieldName", FieldLabels.label(fieldLabels, item.getKey()));
                    row.put("sales", roundMoney(fieldSales));
                    row.put("expenses", roundMoney(fieldExpense));
                    row.put("profit", roundMoney(fieldProfit));
                    row.put("margin", fieldSales <= 0 ? 0 : Math.round(fieldProfit * 1000.0 / fieldSales) / 10.0);
                    rows.add(row);
                });
        return rows;
    }

    private List<Map<String, Object>> buildBudgetRows(int year, List<Sales> sales, List<Expense> expenses,
            List<MonthlyBudget> budgets) {
        Map<Integer, MonthlyBudget> byMonth = new LinkedHashMap<>();
        for (MonthlyBudget budget : budgets) {
            byMonth.put(budget.getBudgetMonth(), budget);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth target = YearMonth.of(year, month);
            double actualSales = sales.stream()
                    .filter(s -> s.getDate() != null && YearMonth.from(s.getDate()).equals(target))
                    .map(Sales::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
            double actualExpenses = expenses.stream()
                    .filter(e -> e.getDate() != null && YearMonth.from(e.getDate()).equals(target))
                    .map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
            MonthlyBudget plan = byMonth.get(month);
            Double salesBudget = plan == null ? null : plan.getSalesBudget();
            Double expenseBudget = plan == null ? null : plan.getExpenseBudget();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("salesBudget", salesBudget == null ? null : roundMoney(salesBudget));
            row.put("salesActual", roundMoney(actualSales));
            row.put("expenseBudget", expenseBudget == null ? null : roundMoney(expenseBudget));
            row.put("expenseActual", roundMoney(actualExpenses));
            row.put("profitBudget", salesBudget == null || expenseBudget == null
                    ? null : roundMoney(salesBudget - expenseBudget));
            row.put("profitActual", roundMoney(actualSales - actualExpenses));
            rows.add(row);
        }
        return rows;
    }

    private boolean validYear(Integer year) {
        return year != null && year >= MIN_YEAR && year <= MAX_YEAR;
    }

    private boolean validMonth(Integer month) {
        return month != null && month >= 1 && month <= 12;
    }

    private boolean invalidMoney(Double value) {
        return value != null && (!Double.isFinite(value) || value < 0 || value > 999_999_999_999.0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private long roundMoney(double value) {
        return Math.round(value);
    }
}
