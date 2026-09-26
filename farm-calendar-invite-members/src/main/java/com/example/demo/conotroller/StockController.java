package com.example.demo.conotroller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.entity.Stock;
import com.example.demo.service.StockService;

@Controller
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("stock", new Stock());
        model.addAttribute("stockList", stockService.findAll());
        return "stock";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Stock stock) {
        stockService.save(stock);
        return "redirect:/stock";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Stock stock = stockService.findById(id);
        if (stock == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("stock", stock);
        model.addAttribute("stockList", stockService.findAll());
        return "stock";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        stockService.delete(id);
        return "redirect:/stock";
    }
}
