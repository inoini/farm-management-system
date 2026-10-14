package com.example.demo.conotroller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.entity.Sales;
import com.example.demo.repository.SalesRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.entity.Field;
import com.example.demo.util.FieldLabels;
import com.example.demo.service.CurrentUserService;

@Controller
public class SalesController {

    private final SalesRepository salesRepository;
    private final FieldRepository fieldRepository;
    private final CurrentUserService currentUser;

    public SalesController(SalesRepository salesRepository, FieldRepository fieldRepository, CurrentUserService currentUser) {
        this.salesRepository = salesRepository;
        this.fieldRepository = fieldRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/sales")
    public String sales(Model model) {
        model.addAttribute("sales", getSales());
        model.addAttribute("sale", new Sales());
        addFields(model);
        return "sales";
    }

    @PostMapping("/sales/add")
    public String add(@ModelAttribute("sale") Sales sale, BindingResult errors, Model model) {
        sale.setId(null);
        validateField(sale, errors);
        if (!errors.hasErrors()) {
            try { sale.calculateAndValidateAmount(); }
            catch (IllegalArgumentException ex) { errors.reject("sale.invalid", ex.getMessage()); }
        }
        if (errors.hasErrors()) {
            model.addAttribute("errorMessage", "入力内容を確認してください。");
            model.addAttribute("sales", getSales());
            addFields(model);
            return "sales";
        }
        sale.setOwnerEmail(currentUser.email());
        salesRepository.save(sale);
        return "redirect:/sales";
    }

    public double getTotal() {
        return getSales().stream().map(Sales::getAmount).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
    }

    public List<Sales> getSales() {
        return salesRepository.findAllByOwnerEmailOrderByDateDescIdDesc(currentUser.email());
    }

    @GetMapping("/sales/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Sales sale = salesRepository.findByIdAndOwnerEmail(id, currentUser.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("sale", sale);
        addFields(model);
        return "sales_edit";
    }

    @PostMapping("/sales/update")
    public String update(@ModelAttribute("sale") Sales sale, BindingResult errors, Model model) {
        String owner = currentUser.email();
        if (sale.getId() == null || salesRepository.findByIdAndOwnerEmail(sale.getId(), owner).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        validateField(sale, errors);
        if (!errors.hasErrors()) {
            try { sale.calculateAndValidateAmount(); }
            catch (IllegalArgumentException ex) { errors.reject("sale.invalid", ex.getMessage()); }
        }
        if (errors.hasErrors()) {
            model.addAttribute("errorMessage", "入力内容を確認してください。");
            model.addAttribute("sales", getSales());
            addFields(model);
            return "sales_edit";
        }
        sale.setOwnerEmail(owner);
        salesRepository.save(sale);
        return "redirect:/sales";
    }

    private void addFields(Model model) {
        List<Field> fields = fieldRepository.findAllByOwnerEmail(currentUser.email()).stream()
                .sorted(java.util.Comparator.comparing(f -> f.getName() == null ? "" : f.getName()))
                .toList();
        model.addAttribute("fields", fields);
        model.addAttribute("fieldLabels", FieldLabels.from(fields));
    }

    private void validateField(Sales sale, BindingResult errors) {
        if (sale.getFieldId() != null
                && !fieldRepository.existsByIdAndOwnerEmail(sale.getFieldId(), currentUser.email())) {
            errors.rejectValue("fieldId", "field.invalid", "登録されている圃場から選択してください。");
        }
    }

    @PostMapping("/sales/delete/{id}")
    public String delete(@PathVariable Long id) {
        salesRepository.findByIdAndOwnerEmail(id, currentUser.email()).ifPresent(salesRepository::delete);
        return "redirect:/sales";
    }
}
