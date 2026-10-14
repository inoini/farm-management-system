package com.example.demo.conotroller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import com.example.demo.entity.Expense;
import com.example.demo.entity.Field;
import com.example.demo.entity.Sales;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.MonthlyBudgetRepository;
import com.example.demo.repository.SalesRepository;
import com.example.demo.service.CurrentUserService;

class FieldProfitAggregationTest {

    @Test
    @SuppressWarnings("unchecked")
    void repeatedTransactionsAddTogetherButDifferentFieldIdsStaySeparate() {
        SalesRepository salesRepository = mock(SalesRepository.class);
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        MonthlyBudgetRepository budgets = mock(MonthlyBudgetRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);

        String owner = "test@example.com";
        when(currentUser.email()).thenReturn(owner);
        when(fieldRepository.findAllByOwnerEmail(owner)).thenReturn(List.of(
                field(7L, "第1圃場"), field(8L, "第1圃場")));
        when(salesRepository.findAllByOwnerEmailOrderByDateDescIdDesc(owner)).thenReturn(List.of(
                sale("さつまいも", 7L, 100),
                sale("さつまいも", 7L, 150),
                sale("じゃがいも", 7L, 80),
                sale("さつまいも", 8L, 200),
                sale("さつまいも", null, 70)));
        when(expenseRepository.findAllByOwnerEmailOrderByDateDescIdDesc(owner)).thenReturn(List.of(
                expense("さつまいも", 7L, 30),
                expense("さつまいも", 7L, 50),
                expense("さつまいも", 8L, 20),
                expense("さつまいも", null, 10),
                expense(null, null, 15)));
        int currentYear = LocalDate.now(ZoneId.of("Asia/Tokyo")).getYear();
        when(budgets.findAllByOwnerEmailAndBudgetYearOrderByBudgetMonthAsc(owner, currentYear))
                .thenReturn(List.of());
        when(budgets.findByOwnerEmailAndBudgetYearAndBudgetMonth(eq(owner), eq(currentYear), anyInt()))
                .thenReturn(Optional.empty());

        ManagementController controller = new ManagementController(
                salesRepository, expenseRepository, fieldRepository, budgets, currentUser);
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("management", controller.management(null, null, model));

        List<Map<String, Object>> fieldRows = (List<Map<String, Object>>) model.get("fieldProfitRows");
        assertEquals(2, fieldRows.size());
        Map<String, Object> first = fieldRows.stream()
                .filter(row -> "第1圃場（ID:7）".equals(row.get("fieldName")))
                .findFirst().orElseThrow();
        assertEquals(330L, first.get("sales"));
        assertEquals(80L, first.get("expenses"));
        assertEquals(250L, first.get("profit"));
        Map<String, Object> second = fieldRows.stream()
                .filter(row -> "第1圃場（ID:8）".equals(row.get("fieldName")))
                .findFirst().orElseThrow();
        assertEquals(200L, second.get("sales"));
        assertEquals(20L, second.get("expenses"));
        assertEquals(180L, second.get("profit"));

        List<Map<String, Object>> cropRows = (List<Map<String, Object>>) model.get("cropProfitRows");
        assertEquals(4, cropRows.size());
        assertTrue(cropRows.stream().anyMatch(row ->
                "さつまいも".equals(row.get("cropName")) &&
                "第1圃場（ID:7）".equals(row.get("fieldName")) &&
                Long.valueOf(170L).equals(row.get("profit"))));
        assertTrue(cropRows.stream().anyMatch(row ->
                "さつまいも".equals(row.get("cropName")) &&
                "圃場未設定".equals(row.get("fieldName")) &&
                Long.valueOf(60L).equals(row.get("profit"))));
        assertEquals(70L, model.get("unassignedFieldSales"));
        assertEquals(25L, model.get("unassignedFieldExpenses"));
        assertEquals(15L, model.get("sharedExpenseTotal"));
    }

    private static Field field(Long id, String name) {
        Field field = new Field();
        field.setId(id);
        field.setName(name);
        return field;
    }

    private static Sales sale(String crop, Long fieldId, double amount) {
        Sales sale = new Sales();
        sale.setCrop(crop);
        sale.setFieldId(fieldId);
        sale.setAmount(amount);
        return sale;
    }

    private static Expense expense(String crop, Long fieldId, double amount) {
        Expense expense = new Expense();
        expense.setCrop(crop);
        expense.setFieldId(fieldId);
        expense.setAmount(amount);
        return expense;
    }
}
