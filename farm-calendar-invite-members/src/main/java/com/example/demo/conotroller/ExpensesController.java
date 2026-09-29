package com.example.demo.conotroller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.entity.Expense;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.service.CurrentUserService;

@Controller
public class ExpensesController {

    private final ExpenseRepository expenseRepository;
    private final CropRepository cropRepository;
    private final CurrentUserService currentUser;

    public ExpensesController(ExpenseRepository expenseRepository, CropRepository cropRepository, CurrentUserService currentUser) {
        this.expenseRepository = expenseRepository;
        this.cropRepository = cropRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/expenses")
    public String expenses(Model model) {
        model.addAttribute("expenses", getExpenses());
        model.addAttribute("expense", new Expense());
        addCropNames(model);
        return "expenses";
    }

    @PostMapping("/expenses/add")
    public String add(@ModelAttribute Expense expense) {
        expense.setId(null);
        expense.setCrop(clean(expense.getCrop()));
        expense.setOwnerEmail(currentUser.email());
        expenseRepository.save(expense);
        return "redirect:/expenses";
    }

    public double getTotal() {
        return getExpenses().stream().filter(e -> e.getAmount() != null).mapToDouble(Expense::getAmount).sum();
    }

    public List<Expense> getExpenses() {
        return expenseRepository.findAllByOwnerEmailOrderByDateDescIdDesc(currentUser.email());
    }

    @GetMapping("/expenses/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Expense expense = expenseRepository.findByIdAndOwnerEmail(id, currentUser.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("expense", expense);
        addCropNames(model);
        return "expenses_edit";
    }

    @PostMapping("/expenses/update")
    public String update(@ModelAttribute Expense expense) {
        String owner = currentUser.email();
        if (expense.getId() == null || expenseRepository.findByIdAndOwnerEmail(expense.getId(), owner).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        expense.setCrop(clean(expense.getCrop()));
        expense.setOwnerEmail(owner);
        expenseRepository.save(expense);
        return "redirect:/expenses";
    }

    @PostMapping("/expenses/delete/{id}")
    public String delete(@PathVariable Long id) {
        expenseRepository.findByIdAndOwnerEmail(id, currentUser.email()).ifPresent(expenseRepository::delete);
        return "redirect:/expenses";
    }

    private void addCropNames(Model model) {
        List<String> cropNames = cropRepository.findAllByOwnerEmailOrderByIdDesc(currentUser.email()).stream()
                .map(crop -> crop.getCropName())
                .filter(name -> name != null && !name.isBlank())
                .map(String::strip)
                .distinct()
                .sorted()
                .toList();
        model.addAttribute("cropNames", cropNames);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
