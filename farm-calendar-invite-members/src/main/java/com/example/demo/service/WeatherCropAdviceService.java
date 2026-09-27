package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.demo.entity.Field;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class WeatherCropAdviceService {

    private static final Logger log = LoggerFactory.getLogger(WeatherCropAdviceService.class);
    private static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RestClient archiveClient;
    private final RestClient forecastClient;
    private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();
    private final Map<String, ThermalProfile> profiles = createProfiles();

    public WeatherCropAdviceService(
            RestClient.Builder builder,
            @Value("${app.weather.archive-base-url:https://archive-api.open-meteo.com}") String archiveBaseUrl,
            @Value("${app.weather.forecast-base-url:https://api.open-meteo.com}") String forecastBaseUrl) {
        this.archiveClient = builder.clone().baseUrl(archiveBaseUrl).build();
        this.forecastClient = builder.clone().baseUrl(forecastBaseUrl).build();
    }

    /**
     * 植付後の実績気象と短期予報を使って、登録済み収穫予定日の天候補正目安を返します。
     * 長期予報が存在しない期間は作物ごとの標準的な積算温度ペースで補間します。
     */
    public Optional<Advice> analyze(String cropName, String variety, LocalDate plantingDate,
            LocalDate plannedHarvestDate, Field field, boolean forceRefresh) {

        if (cropName == null || cropName.isBlank() || plantingDate == null || plannedHarvestDate == null
                || field == null || field.getLatitude() == null || field.getLongitude() == null) {
            return Optional.empty();
        }
        if (plannedHarvestDate.isBefore(plantingDate)) {
            return Optional.empty();
        }

        ThermalProfile profile = findProfile(cropName);
        LocalDate today = LocalDate.now(JAPAN);
        Map<LocalDate, WeatherDay> weather = loadWeather(field.getLatitude(), field.getLongitude(), plantingDate, today, forceRefresh);

        long nominalDays = Math.max(1, ChronoUnit.DAYS.between(plantingDate, plannedHarvestDate));
        double targetGdd = nominalDays * profile.standardDailyGdd();
        LocalDate scanEnd = maxDate(plannedHarvestDate.plusDays(30), today.plusDays(16));
        scanEnd = maxDate(scanEnd, plantingDate.plusDays(Math.min(365, nominalDays + 30)));

        double cumulativeGdd = 0.0;
        LocalDate thermalEstimate = plannedHarvestDate;
        boolean reached = false;

        for (LocalDate day = plantingDate; !day.isAfter(scanEnd); day = day.plusDays(1)) {
            WeatherDay actualOrForecast = weather.get(day);
            double dailyGdd = actualOrForecast == null
                    ? profile.standardDailyGdd()
                    : growingDegreeDay(actualOrForecast.meanTemperature(), profile.baseTemperature());
            cumulativeGdd += dailyGdd;
            if (cumulativeGdd >= targetGdd) {
                thermalEstimate = day;
                reached = true;
                break;
            }
        }

        if (!reached) {
            thermalEstimate = plannedHarvestDate.plusDays(21);
        }

        // 目安が極端に動かないよう、一般的な予定日の前後21日までに制限します。
        LocalDate minDate = plannedHarvestDate.minusDays(21);
        LocalDate maxDate = plannedHarvestDate.plusDays(21);
        LocalDate adjusted = clamp(thermalEstimate, minDate, maxDate);

        // 根菜・いも類などは、収穫予定付近で強い雨が予報される場合だけ乾いた日に寄せます。
        if (profile.preferDryHarvest()) {
            adjusted = chooseDryHarvestDay(adjusted, weather, minDate, maxDate);
        }

        List<WeatherDay> observed = weather.values().stream()
                .filter(day -> !day.date().isBefore(plantingDate) && !day.date().isAfter(today))
                .sorted(Comparator.comparing(WeatherDay::date))
                .toList();

        Double meanTemperature = observed.isEmpty() ? null
                : observed.stream().mapToDouble(WeatherDay::meanTemperature).average().orElse(Double.NaN);
        Double rainMm = observed.isEmpty() ? null
                : observed.stream().mapToDouble(WeatherDay::precipitation).sum();
        LocalDate observedThrough = observed.isEmpty() ? null : observed.get(observed.size() - 1).date();
        LocalDate forecastThrough = weather.values().stream()
                .map(WeatherDay::date)
                .filter(date -> date.isAfter(today))
                .max(LocalDate::compareTo)
                .orElse(null);

        long shiftDays = ChronoUnit.DAYS.between(plannedHarvestDate, adjusted);
        String summary = buildSummary(cropName, variety, plannedHarvestDate, adjusted, shiftDays,
                meanTemperature, rainMm, observedThrough, forecastThrough, today);

        return Optional.of(new Advice(
                adjusted,
                summary,
                LocalDateTime.now(JAPAN).withNano(0),
                observedThrough,
                forecastThrough,
                meanTemperature,
                rainMm,
                shiftDays,
                "Open-Meteo"));
    }

    private Map<LocalDate, WeatherDay> loadWeather(double latitude, double longitude,
            LocalDate plantingDate, LocalDate today, boolean forceRefresh) {
        String key = String.format(Locale.ROOT, "%.4f:%.4f:%s:%s", latitude, longitude, plantingDate, today);
        CacheEntry cached = weatherCache.get(key);
        if (!forceRefresh && cached != null
                && Duration.between(cached.createdAt(), LocalDateTime.now(JAPAN)).compareTo(CACHE_TTL) < 0) {
            return cached.days();
        }

        Map<LocalDate, WeatherDay> merged = new LinkedHashMap<>();

        // Historical Weather APIは直近データに遅延が生じる場合があるため、8日前までを取得。
        LocalDate archiveEnd = today.minusDays(8);
        if (!plantingDate.isAfter(archiveEnd)) {
            try {
                OpenMeteoResponse archive = archiveClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/archive")
                                .queryParam("latitude", latitude)
                                .queryParam("longitude", longitude)
                                .queryParam("start_date", plantingDate)
                                .queryParam("end_date", archiveEnd)
                                .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum")
                                .queryParam("timezone", "Asia/Tokyo")
                                .build())
                        .retrieve()
                        .body(OpenMeteoResponse.class);
                mergeResponse(merged, archive);
            } catch (RuntimeException ex) {
                log.warn("作物の過去気象データを取得できませんでした。", ex);
            }
        }

        // 直近7日＋最大16日先を取得し、過去APIとの隙間を埋めます。
        try {
            OpenMeteoResponse forecast = forecastClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum")
                            .queryParam("past_days", 7)
                            .queryParam("forecast_days", 16)
                            .queryParam("timezone", "Asia/Tokyo")
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponse.class);
            mergeResponse(merged, forecast);
        } catch (RuntimeException ex) {
            log.warn("作物の予報データを取得できませんでした。", ex);
        }

        Map<LocalDate, WeatherDay> result = Map.copyOf(merged);
        weatherCache.put(key, new CacheEntry(LocalDateTime.now(JAPAN), result));
        if (weatherCache.size() > 150) {
            weatherCache.entrySet().removeIf(entry -> Duration
                    .between(entry.getValue().createdAt(), LocalDateTime.now(JAPAN))
                    .compareTo(CACHE_TTL.multipliedBy(2)) > 0);
        }
        return result;
    }

    private void mergeResponse(Map<LocalDate, WeatherDay> target, OpenMeteoResponse response) {
        if (response == null || response.daily() == null || response.daily().time() == null) return;
        Daily daily = response.daily();
        int size = daily.time().size();
        for (int i = 0; i < size; i++) {
            try {
                LocalDate date = LocalDate.parse(daily.time().get(i));
                Double max = valueAt(daily.maxTemperature(), i);
                Double min = valueAt(daily.minTemperature(), i);
                Double precipitation = valueAt(daily.precipitation(), i);
                if (max == null || min == null) continue;
                target.put(date, new WeatherDay(date, (max + min) / 2.0, precipitation == null ? 0.0 : precipitation));
            } catch (RuntimeException ignored) {
                // 不完全な1日分だけを無視し、他の日付は利用します。
            }
        }
    }

    private Double valueAt(List<Double> values, int index) {
        if (values == null || index < 0 || index >= values.size()) return null;
        return values.get(index);
    }

    private double growingDegreeDay(double meanTemperature, double baseTemperature) {
        return Math.max(0.0, meanTemperature - baseTemperature);
    }

    private LocalDate chooseDryHarvestDay(LocalDate estimate, Map<LocalDate, WeatherDay> weather,
            LocalDate minDate, LocalDate maxDate) {
        WeatherDay estimateWeather = weather.get(estimate);
        if (estimateWeather == null || estimateWeather.precipitation() < 8.0) {
            return estimate;
        }
        for (int offset = 1; offset <= 5; offset++) {
            LocalDate candidate = estimate.plusDays(offset);
            if (candidate.isAfter(maxDate)) break;
            WeatherDay candidateWeather = weather.get(candidate);
            if (candidateWeather != null && candidateWeather.precipitation() <= 3.0) {
                return clamp(candidate, minDate, maxDate);
            }
        }
        return estimate;
    }

    private String buildSummary(String cropName, String variety, LocalDate planned, LocalDate adjusted,
            long shiftDays, Double meanTemperature, Double rainMm, LocalDate observedThrough,
            LocalDate forecastThrough, LocalDate today) {
        List<String> parts = new ArrayList<>();
        String cropLabel = (variety == null || variety.isBlank()) ? cropName : cropName + "（" + variety.strip() + "）";
        parts.add(cropLabel + "の植付後の気温推移と降水量から収穫目安を補正しました。");
        if (meanTemperature != null && !meanTemperature.isNaN()) {
            parts.add(String.format(Locale.JAPAN, "実績期間の平均気温は約%.1f℃", meanTemperature));
        }
        if (rainMm != null) {
            parts.add(String.format(Locale.JAPAN, "積算降水量は約%.0fmmです", rainMm));
        }
        if (shiftDays == 0) {
            parts.add("現在の天候では登録済み収穫予定日から大きな補正はありません。");
        } else if (shiftDays > 0) {
            parts.add("登録済み予定日より約" + shiftDays + "日遅い " + adjusted + " を目安にしています。");
        } else {
            parts.add("登録済み予定日より約" + Math.abs(shiftDays) + "日早い " + adjusted + " を目安にしています。");
        }
        if (observedThrough != null) {
            parts.add("実績天気は" + observedThrough + "まで反映しています。");
        }
        if (forecastThrough != null) {
            parts.add("予報は" + forecastThrough + "まで反映しています。");
        }
        if (planned.isAfter(today.plusDays(16))) {
            parts.add("16日より先は確定予報がないため、作物ごとの標準的な積算温度ペースで補間しています。");
        }
        parts.add("生育状態、土壌水分、病害虫、収穫物の成熟状態も確認して最終判断してください。");
        return String.join(" ", parts);
    }

    private ThermalProfile findProfile(String cropName) {
        String normalized = normalize(cropName);
        ThermalProfile exact = profiles.get(normalized);
        if (exact != null) return exact;
        return profiles.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()) || entry.getKey().contains(normalized))
                .max(Comparator.comparingInt(entry -> entry.getKey().length()))
                .map(Map.Entry::getValue)
                .orElse(new ThermalProfile(7.0, 8.0, false));
    }

    private Map<String, ThermalProfile> createProfiles() {
        Map<String, ThermalProfile> map = new HashMap<>();
        add(map, new ThermalProfile(10, 10, true), "さつまいも", "甘藷", "かんしょ");
        add(map, new ThermalProfile(5, 9, true), "じゃがいも", "馬鈴薯", "ばれいしょ");
        add(map, new ThermalProfile(4, 8, true), "にんじん", "人参", "だいこん", "大根", "かぶ", "蕪");
        add(map, new ThermalProfile(10, 11, false), "とうもろこし", "コーン");
        add(map, new ThermalProfile(10, 10, false), "トマト", "ミニトマト", "とまと", "みにとまと", "なす", "茄子", "きゅうり", "胡瓜", "ピーマン", "パプリカ", "かぼちゃ", "南瓜", "ズッキーニ", "オクラ", "すいか", "西瓜", "メロン");
        add(map, new ThermalProfile(8, 9, false), "えだまめ", "枝豆");
        add(map, new ThermalProfile(10, 9, true), "落花生", "らっかせい", "ピーナッツ");
        add(map, new ThermalProfile(5, 8, false), "キャベツ", "白菜", "はくさい", "ブロッコリー", "レタス", "ほうれんそう", "ほうれん草", "小松菜", "こまつな");
        add(map, new ThermalProfile(5, 7, true), "ねぎ", "葱", "長ねぎ", "玉ねぎ", "たまねぎ", "玉葱", "にんにく", "大蒜");
        add(map, new ThermalProfile(10, 9, true), "さといも", "里芋", "しょうが", "生姜");
        return Map.copyOf(map);
    }

    private void add(Map<String, ThermalProfile> map, ThermalProfile profile, String... names) {
        for (String name : names) map.put(normalize(name), profile);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.strip().toLowerCase(Locale.JAPAN)
                .replace(" ", "").replace("　", "").replace("・", "").replace("･", "");
    }

    private LocalDate clamp(LocalDate value, LocalDate min, LocalDate max) {
        if (value.isBefore(min)) return min;
        if (value.isAfter(max)) return max;
        return value;
    }

    private LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    public record Advice(
            LocalDate adjustedHarvestDate,
            String summary,
            LocalDateTime analyzedAt,
            LocalDate observedThrough,
            LocalDate forecastThrough,
            Double meanTemperature,
            Double rainMm,
            long shiftDays,
            String source) {
    }

    private record ThermalProfile(double baseTemperature, double standardDailyGdd, boolean preferDryHarvest) {
    }

    private record WeatherDay(LocalDate date, double meanTemperature, double precipitation) {
    }

    private record CacheEntry(LocalDateTime createdAt, Map<LocalDate, WeatherDay> days) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenMeteoResponse(Daily daily) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Daily(
            List<String> time,
            @JsonProperty("temperature_2m_max") List<Double> maxTemperature,
            @JsonProperty("temperature_2m_min") List<Double> minTemperature,
            @JsonProperty("precipitation_sum") List<Double> precipitation) {
    }
}
