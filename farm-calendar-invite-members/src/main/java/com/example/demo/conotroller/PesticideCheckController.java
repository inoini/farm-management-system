package com.example.demo.conotroller;

import com.example.demo.entity.Stock;
import com.example.demo.service.PesticideCheckService;
import com.example.demo.service.StockService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PesticideCheckController {
    private final StockService stockService;
    private final PesticideCheckService checkService;

    public PesticideCheckController(StockService stockService, PesticideCheckService checkService) {
        this.stockService = stockService;
        this.checkService = checkService;
    }

    @GetMapping("/stock/pesticide-check/{id}")
    public String check(@PathVariable Long id,
            @RequestParam(name = "crop", required = false) String crop,
            @RequestParam(name = "dilution", required = false) String dilution,
            @RequestParam(name = "applicationDate", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applicationDate,
            @RequestParam(name = "harvestDate", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate harvestDate,
            @RequestParam(name = "run", defaultValue = "false") boolean run,
            Model model) {
        // StockService enforces the current farm's owner key.
        Stock stock = stockService.findById(id);
        if (stock == null || !"農薬".equals(stock.getCategory())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("stockItem", stock);
        model.addAttribute("crop", crop == null ? stock.getTargetCrop() : crop);
        model.addAttribute("dilution", dilution == null ? stock.getDilutionRate() : dilution);
        model.addAttribute("applicationDate", applicationDate == null && !run
                ? LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")) : applicationDate);
        model.addAttribute("harvestDate", harvestDate);
        if (run) {
            model.addAttribute("report", checkService.check(stock, crop, dilution, applicationDate, harvestDate));
        }
        return "pesticide-check";
    }
}
