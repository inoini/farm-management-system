package com.example.demo.conotroller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Expense;
import com.example.demo.entity.MonthlyBudget;
import com.example.demo.entity.Sales;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.MonthlyBudgetRepository;
import com.example.demo.repository.SalesRepository;
import com.example.demo.service.CurrentUserService;

@Controller
public class ManagementController {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    private final SalesRepository salesRepository;
    private final ExpenseRepository expenseRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final CurrentUserService currentUser;

    public ManagementController(SalesRepository salesRepository,
            ExpenseRepository expenseRepository,
            MonthlyBudgetRepository budgetRepository,
            CurrentUserService currentUser) {
        this.salesRepository = salesRepository;
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/management")
    public String management(Model model) {
        String owner = currentUser.email();
        LocalDate today = LocalDate.now(TOKYO);
        int year = today.getYear();
        int month = today.getMonthValue();

        List<Sales> sales = salesRepository.findAllByOwnerEmailOrderByDateDescIdDesc(owner);
        List<Expense> expenses = expenseRepository.findAllByOwnerEmailOrderByDateDescIdDesc(owner);
        List<MonthlyBudget> budgets = budgetRepository.findAllByOwnerEmailAndBudgetYearOrderByBudgetMonthAsc(owner, year);

        model.addAttribute("currentYear", year);
        model.addAttribute("currentMonth", month);
        model.addAttribute("cropProfitRows", buildCropProfitRows(sales, expenses));
        model.addAttribute("sharedExpenseTotal", roundMoney(expenses.stream()
                .filter(e -> normalize(e.getCrop()).isEmpty())
                .map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum()));

        MonthlyBudget budgetForm = new MonthlyBudget();
        budgetForm.setBudgetYear(year);
        budgetForm.setBudgetMonth(month);
        budgetRepository.findByOwnerEmailAndBudgetYearAndBudgetMonth(owner, year, month).ifPresent(existing -> {
            budgetForm.setSalesBudget(existing.getSalesBudget());
            budgetForm.setExpenseBudget(existing.getExpenseBudget());
        });
        model.addAttribute("budgetForm", budgetForm);
        model.addAttribute("budgetRows", buildBudgetRows(year, sales, expenses, budgets));

        double ytdSales = sales.stream()
                .filter(s -> isCurrentYearActual(s.getDate(), today))
                .map(Sales::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double ytdExpenses = expenses.stream()
                .filter(e -> isCurrentYearActual(e.getDate(), today))
                .map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();

        double annualSalesForecast = month == 0 ? 0 : ytdSales / month * 12.0;
        double annualExpenseForecast = month == 0 ? 0 : ytdExpenses / month * 12.0;
        double annualBudgetSales = budgets.stream().map(MonthlyBudget::getSalesBudget)
                .filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double annualBudgetExpenses = budgets.stream().map(MonthlyBudget::getExpenseBudget)
                .filter(v -> v != null).mapToDouble(Double::doubleValue).sum();

        model.addAttribute("ytdSales", roundMoney(ytdSales));
        model.addAttribute("ytdExpenses", roundMoney(ytdExpenses));
        model.addAttribute("ytdProfit", roundMoney(ytdSales - ytdExpenses));
        model.addAttribute("annualSalesForecast", roundMoney(annualSalesForecast));
        model.addAttribute("annualExpenseForecast", roundMoney(annualExpenseForecast));
        model.addAttribute("annualProfitForecast", roundMoney(annualSalesForecast - annualExpenseForecast));
        model.addAttribute("annualBudgetSales", roundMoney(annualBudgetSales));
        model.addAttribute("annualBudgetExpenses", roundMoney(annualBudgetExpenses));
        model.addAttribute("annualBudgetProfit", roundMoney(annualBudgetSales - annualBudgetExpenses));

        return "management";
    }

    @PostMapping("/management/budget/save")
    public String saveBudget(@ModelAttribute MonthlyBudget input, RedirectAttributes redirectAttributes) {
        Integer year = input.getBudgetYear();
        Integer month = input.getBudgetMonth();
        if (year == null || year < 2000 || year > 2100 || month == null || month < 1 || month > 12
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
        budget.setSalesBudget(input.getSalesBudget() == null ? 0.0 : input.getSalesBudget());
        budget.setExpenseBudget(input.getExpenseBudget() == null ? 0.0 : input.getExpenseBudget());
        budgetRepository.save(budget);
        redirectAttributes.addFlashAttribute("budgetSaved", year + "年" + month + "月の予算を保存しました。");
        return "redirect:/management#budget";
    }

    private List<Map<String, Object>> buildCropProfitRows(List<Sales> sales, List<Expense> expenses) {
        Set<String> names = new LinkedHashSet<>();
        sales.stream().map(Sales::getCrop).map(this::normalize).filter(v -> !v.isEmpty()).sorted().forEach(names::add);
        expenses.stream().map(Expense::getCrop).map(this::normalize).filter(v -> !v.isEmpty()).sorted().forEach(names::add);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String name : names) {
            double cropSales = sales.stream()
                    .filter(s -> name.equals(normalize(s.getCrop())))
                    .map(Sales::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
            double cropExpense = expenses.stream()
                    .filter(e -> name.equals(normalize(e.getCrop())))
                    .map(Expense::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
            double cropProfit = cropSales - cropExpense;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cropName", name);
            row.put("sales", roundMoney(cropSales));
            row.put("expenses", roundMoney(cropExpense));
            row.put("profit", roundMoney(cropProfit));
            row.put("margin", cropSales <= 0 ? 0 : Math.round(cropProfit * 1000.0 / cropSales) / 10.0);
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildBudgetRows(int year, List<Sales> sales, List<Expense> expenses,
            List<MonthlyBudget> budgets) {
        Map<Integer, MonthlyBudget> byMonth = new LinkedHashMap<>();
        for (MonthlyBudget budget : budgets) byMonth.put(budget.getBudgetMonth(), budget);

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
            double salesBudget = plan == null || plan.getSalesBudget() == null ? 0 : plan.getSalesBudget();
            double expenseBudget = plan == null || plan.getExpenseBudget() == null ? 0 : plan.getExpenseBudget();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("salesBudget", roundMoney(salesBudget));
            row.put("salesActual", roundMoney(actualSales));
            row.put("expenseBudget", roundMoney(expenseBudget));
            row.put("expenseActual", roundMoney(actualExpenses));
            row.put("profitBudget", roundMoney(salesBudget - expenseBudget));
            row.put("profitActual", roundMoney(actualSales - actualExpenses));
            rows.add(row);
        }
        return rows;
    }

    private boolean isCurrentYearActual(LocalDate date, LocalDate today) {
        return date != null && date.getYear() == today.getYear() && !date.isAfter(today);
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
