package com.example.demo.conotroller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.entity.Crop;
import com.example.demo.entity.Field;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.WeatherCropAdviceService;
import com.example.demo.service.WeatherCropAdviceService.Advice;

@Controller
public class CropController {

    private final CropRepository cropRepository;
    private final FieldRepository fieldRepository;
    private final CurrentUserService currentUser;
    private final WeatherCropAdviceService weatherAdviceService;

    public CropController(CropRepository cropRepository, FieldRepository fieldRepository,
            CurrentUserService currentUser, WeatherCropAdviceService weatherAdviceService) {
        this.cropRepository = cropRepository;
        this.fieldRepository = fieldRepository;
        this.currentUser = currentUser;
        this.weatherAdviceService = weatherAdviceService;
    }

    @GetMapping("/crop")
    public String cropList(Model model) {
        model.addAttribute("crops", cropRepository.findAllByOwnerEmailOrderByIdDesc(currentUser.email()));
        model.addAttribute("crop", new Crop());
        return "crop/list";
    }

    @GetMapping("/crop/add")
    public String cropAdd(Model model) {
        model.addAttribute("crop", new Crop());
        addFields(model);
        return "crop/add";
    }

    @GetMapping("/crop/edit/{id}")
    public String editCrop(@PathVariable Long id, Model model) {
        Crop crop = cropRepository.findByIdAndOwnerEmail(id, currentUser.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("crop", crop);
        addFields(model);
        return "crop/add";
    }

    @PostMapping("/crop/save")
    public String saveCrop(@ModelAttribute Crop crop) {
        String owner = currentUser.email();
        if (crop.getId() != null && cropRepository.findByIdAndOwnerEmail(crop.getId(), owner).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        crop.setOwnerEmail(owner);
        cropRepository.save(crop);
        return "redirect:/crop";
    }

    @PostMapping("/crop/delete/{id}")
    public String deleteCrop(@PathVariable Long id) {
        cropRepository.findByIdAndOwnerEmail(id, currentUser.email()).ifPresent(cropRepository::delete);
        return "redirect:/crop";
    }

    @GetMapping("/crop/weather-advice")
    @ResponseBody
    public WeatherAdviceResponse weatherAdvice(
            @RequestParam String cropName,
            @RequestParam(required = false) String variety,
            @RequestParam LocalDate plantingDate,
            @RequestParam LocalDate harvestDate,
            @RequestParam String fieldName,
            @RequestParam(required = false) Long refresh) {

        Field field = findField(fieldName);
        if (field == null) {
            return WeatherAdviceResponse.unavailable(
                    "天候補正には、圃場管理に登録済みの圃場名を選択してください。");
        }
        if (field.getLatitude() == null || field.getLongitude() == null) {
            return WeatherAdviceResponse.unavailable(
                    "この圃場は地図位置を取得できていません。圃場管理で住所を確認して保存し直してください。");
        }

        return weatherAdviceService.analyze(cropName, variety, plantingDate, harvestDate, field, refresh != null)
                .map(WeatherAdviceResponse::available)
                .orElseGet(() -> WeatherAdviceResponse.unavailable(
                        "天候補正に必要な情報を取得できませんでした。一般的な収穫目安はそのまま利用できます。"));
    }

    private Field findField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) return null;
        String normalized = fieldName.strip();
        return fieldRepository.findAllByOwnerEmail(currentUser.email()).stream()
                .filter(field -> field.getName() != null && field.getName().strip().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    private void addFields(Model model) {
        List<Field> fields = fieldRepository.findAllByOwnerEmail(currentUser.email()).stream()
                .sorted(Comparator.comparing(Field::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        model.addAttribute("fields", fields);
    }

    public record WeatherAdviceResponse(
            boolean available,
            String message,
            LocalDate adjustedHarvestDate,
            String summary,
            LocalDateTime analyzedAt,
            LocalDate observedThrough,
            LocalDate forecastThrough,
            Double meanTemperature,
            Double rainMm,
            Long shiftDays,
            String source) {

        static WeatherAdviceResponse available(Advice advice) {
            return new WeatherAdviceResponse(
                    true,
                    "天候データを反映しました。",
                    advice.adjustedHarvestDate(),
                    advice.summary(),
                    advice.analyzedAt(),
                    advice.observedThrough(),
                    advice.forecastThrough(),
                    advice.meanTemperature(),
                    advice.rainMm(),
                    advice.shiftDays(),
                    advice.source());
        }

        static WeatherAdviceResponse unavailable(String message) {
            return new WeatherAdviceResponse(false, message, null, null, null, null, null, null, null, null, null);
        }
    }
}
