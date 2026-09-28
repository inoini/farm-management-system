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
import com.example.demo.service.CropPlanningService;
import com.example.demo.service.CropPlanningService.Plan;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.WeatherCropAdviceService;
import com.example.demo.service.WeatherCropAdviceService.Advice;

@Controller
public class CropController {

    private final CropRepository cropRepository;
    private final FieldRepository fieldRepository;
    private final CurrentUserService currentUser;
    private final WeatherCropAdviceService weatherAdviceService;
    private final CropPlanningService cropPlanningService;

    public CropController(CropRepository cropRepository, FieldRepository fieldRepository,
            CurrentUserService currentUser, WeatherCropAdviceService weatherAdviceService,
            CropPlanningService cropPlanningService) {
        this.cropRepository = cropRepository;
        this.fieldRepository = fieldRepository;
        this.currentUser = currentUser;
        this.weatherAdviceService = weatherAdviceService;
        this.cropPlanningService = cropPlanningService;
    }

    @GetMapping("/crop")
    public String cropList(Model model) {
        List<Crop> crops = cropRepository.findAllByOwnerEmailOrderByIdDesc(currentUser.email());
        // 既存データに長い追肥文が保存されていても、一覧ではコンパクトな「○日後」表示に統一する。
        crops.forEach(crop -> {
            String compact = cropPlanningService.compactTopdressingGuide(crop.getCropName(), crop.getVariety());
            if (compact != null && !compact.isBlank()) {
                crop.setTopdressingGuide(compact);
            }
        });
        model.addAttribute("crops", crops);
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
        Crop existing = null;
        if (crop.getId() != null) {
            existing = cropRepository.findByIdAndOwnerEmail(crop.getId(), owner)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            preserveMissingEditValues(crop, existing);
        }

        crop.setOwnerEmail(owner);
        applyPlanning(crop);
        // 収穫予定日は入力させず、作物・植付日から算出し、天候補正日も表示用に保存する。
        applyWeatherPrediction(crop, false, false);
        cropRepository.save(crop);
        return "redirect:/crop";
    }

    /**
     * 一覧の「更新」ボタン用。最新天気・植付日・面積から再計算し、
     * 収穫予定日、追肥目安、収穫予想量をまとめて保存する。
     */
    @PostMapping("/crop/refresh/{id}")
    public String refreshCropPlan(@PathVariable Long id) {
        Crop crop = cropRepository.findByIdAndOwnerEmail(id, currentUser.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        applyPlanning(crop);
        applyWeatherPrediction(crop, true, false);
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
            @RequestParam(required = false) LocalDate harvestDate,
            @RequestParam String fieldName,
            @RequestParam(required = false) Double area,
            @RequestParam(required = false) Long refresh) {

        Plan plan = cropPlanningService.plan(cropName, variety, plantingDate, area);
        // 自動予測では植付日から算出した標準日を基準にする。更新を繰り返しても補正が累積しない。
        LocalDate plannedHarvestDate = plan.suggestedHarvestDate() != null
                ? plan.suggestedHarvestDate()
                : harvestDate;
        if (plannedHarvestDate == null) {
            return WeatherAdviceResponse.unavailable("作物名と植付日から収穫予定日を計算できませんでした。");
        }

        Field field = findField(fieldName);
        if (field == null) {
            return WeatherAdviceResponse.unavailable(
                    "天候補正には、圃場管理に登録済みの圃場名を選択してください。");
        }
        if (field.getLatitude() == null || field.getLongitude() == null) {
            return WeatherAdviceResponse.unavailable(
                    "この圃場は地図位置を取得できていません。圃場管理で住所を確認して保存し直してください。");
        }

        final LocalDate planned = plannedHarvestDate;
        return weatherAdviceService.analyze(cropName, variety, plantingDate, planned, field, refresh != null)
                .map(advice -> WeatherAdviceResponse.available(advice, plan.topdressingGuide(), plan.expectedHarvestKg()))
                .orElseGet(() -> WeatherAdviceResponse.unavailable(
                        "天候補正に必要な情報を取得できませんでした。一般的な収穫目安はそのまま利用できます。"));
    }

    private void applyPlanning(Crop crop) {
        Plan plan = cropPlanningService.plan(
                crop.getCropName(), crop.getVariety(), crop.getPlantingDate(), crop.getArea());

        if (plan.topdressingGuide() != null && !plan.topdressingGuide().isBlank()) {
            crop.setTopdressingGuide(plan.topdressingGuide());
        }
        // 収穫予定日は手入力ではなく、作物・品種・植付日から毎回算出する。
        // 対応データがない場合は古い予測値を残さず未設定にする。
        crop.setHarvestDate(plan.suggestedHarvestDate());
        if (plan.expectedHarvestKg() != null) {
            crop.setExpectedHarvestKg(plan.expectedHarvestKg());
        }
    }

    private void applyWeatherPrediction(Crop crop, boolean forceRefresh, boolean promoteToHarvestDate) {
        if (crop.getCropName() == null || crop.getCropName().isBlank()
                || crop.getPlantingDate() == null || crop.getHarvestDate() == null
                || crop.getFieldName() == null || crop.getFieldName().isBlank()) {
            return;
        }

        Field field = findField(crop.getFieldName());
        if (field == null || field.getLatitude() == null || field.getLongitude() == null) {
            return;
        }

        Plan plan = cropPlanningService.plan(
                crop.getCropName(), crop.getVariety(), crop.getPlantingDate(), crop.getArea());
        LocalDate weatherBaseDate = plan.suggestedHarvestDate() != null
                ? plan.suggestedHarvestDate()
                : crop.getHarvestDate();
        if (weatherBaseDate == null) return;

        weatherAdviceService.analyze(
                crop.getCropName(), crop.getVariety(), crop.getPlantingDate(), weatherBaseDate, field, forceRefresh)
                .ifPresent(advice -> {
                    crop.setWeatherAdjustedHarvestDate(advice.adjustedHarvestDate());
                    crop.setWeatherAdvice(advice.summary());
                    crop.setWeatherAnalyzedAt(advice.analyzedAt());
                    if (promoteToHarvestDate && advice.adjustedHarvestDate() != null) {
                        crop.setHarvestDate(advice.adjustedHarvestDate());
                    }
                });
    }

    /**
     * 編集フォームの一部値がブラウザ/PWA側で欠けても、既存の植付日等を消さない。
     */
    private void preserveMissingEditValues(Crop incoming, Crop existing) {
        if (incoming.getPlantingDate() == null) incoming.setPlantingDate(existing.getPlantingDate());
        if (incoming.getHarvestDate() == null) incoming.setHarvestDate(existing.getHarvestDate());
        if (incoming.getWeatherAdjustedHarvestDate() == null) {
            incoming.setWeatherAdjustedHarvestDate(existing.getWeatherAdjustedHarvestDate());
        }
        if (incoming.getWeatherAdvice() == null || incoming.getWeatherAdvice().isBlank()) {
            incoming.setWeatherAdvice(existing.getWeatherAdvice());
        }
        if (incoming.getWeatherAnalyzedAt() == null) incoming.setWeatherAnalyzedAt(existing.getWeatherAnalyzedAt());
        if (incoming.getTopdressingGuide() == null || incoming.getTopdressingGuide().isBlank()) {
            incoming.setTopdressingGuide(existing.getTopdressingGuide());
        }
        if (incoming.getExpectedHarvestKg() == null) incoming.setExpectedHarvestKg(existing.getExpectedHarvestKg());
        if (incoming.getArea() == null) incoming.setArea(existing.getArea());
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
            String source,
            String topdressingGuide,
            Double expectedHarvestKg) {

        static WeatherAdviceResponse available(Advice advice, String topdressingGuide, Double expectedHarvestKg) {
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
                    advice.source(),
                    topdressingGuide,
                    expectedHarvestKg);
        }

        static WeatherAdviceResponse unavailable(String message) {
            return new WeatherAdviceResponse(false, message, null, null, null, null, null, null, null, null, null, null, null);
        }
    }
}
