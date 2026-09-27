package com.example.demo.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * 作物名・品種・植付日・面積から、栽培予定の概算値を作るサービス。
 * 数値は栽培計画用の目安で、地域・作型・土壌・施肥・品種で変動する。
 */
@Service
public class CropPlanningService {

    private final Map<String, Profile> profiles = createProfiles();

    public Plan plan(String cropName, String variety, LocalDate plantingDate, Double areaAre) {
        Profile profile = findProfile(cropName);
        if (profile == null) {
            return new Plan(null, null, null, null);
        }

        int harvestDays = resolveHarvestDays(profile, variety);
        LocalDate suggestedHarvestDate = plantingDate == null ? null : plantingDate.plusDays(harvestDays);
        String topdressingGuide = compactTopdressing(profile.topdressingDays());
        Double expectedHarvestKg = null;
        if (areaAre != null && areaAre > 0 && profile.yieldKgPerAre() > 0) {
            expectedHarvestKg = roundOneDecimal(areaAre * profile.yieldKgPerAre());
        }
        return new Plan(suggestedHarvestDate, topdressingGuide, expectedHarvestKg, profile.yieldKgPerAre());
    }

    public String compactTopdressingGuide(String cropName, String variety) {
        Profile profile = findProfile(cropName);
        if (profile == null) return null;
        return compactTopdressing(profile.topdressingDays());
    }

    private String compactTopdressing(int[] days) {
        if (days == null || days.length == 0) {
            return "生育を見て調整";
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            if (i > 0) text.append("・");
            text.append(days[i]).append("日後");
        }
        return text.toString();
    }

    private int resolveHarvestDays(Profile profile, String variety) {
        String normalizedVariety = normalize(variety);
        if (!normalizedVariety.isBlank()) {
            for (Map.Entry<String, Integer> entry : profile.varietyHarvestDays().entrySet()) {
                String key = entry.getKey();
                if (normalizedVariety.equals(key) || normalizedVariety.contains(key) || key.contains(normalizedVariety)) {
                    return entry.getValue();
                }
            }
        }
        return profile.harvestDays();
    }

    private Profile findProfile(String cropName) {
        String normalized = normalize(cropName);
        if (normalized.isBlank()) return null;
        Profile exact = profiles.get(normalized);
        if (exact != null) return exact;

        Profile best = null;
        int bestLength = -1;
        for (Map.Entry<String, Profile> entry : profiles.entrySet()) {
            String key = entry.getKey();
            if ((normalized.contains(key) || key.contains(normalized)) && key.length() > bestLength) {
                best = entry.getValue();
                bestLength = key.length();
            }
        }
        return best;
    }

    private Map<String, Profile> createProfiles() {
        Map<String, Profile> map = new LinkedHashMap<>();

        add(map, profile(135, new int[]{38}, 250,
                varieties("紅はるか", 140, "べにはるか", 140, "シルクスイート", 130)),
                "さつまいも", "甘藷", "かんしょ");
        add(map, profile(100, new int[]{30, 50}, 250,
                varieties("キタアカリ", 93, "男爵", 98, "男爵薯", 98, "メークイン", 110)),
                "じゃがいも", "馬鈴薯", "ばれいしょ");
        add(map, profile(110, new int[]{35, 60}, 300), "にんじん", "人参");
        add(map, profile(75, new int[]{25, 45}, 400), "だいこん", "大根");
        add(map, profile(30, new int[]{15}, 180), "ラディッシュ", "はつかだいこん", "二十日大根");
        add(map, profile(55, new int[]{25}, 250), "かぶ", "蕪");
        add(map, profile(90, new int[]{35, 55}, 110), "とうもろこし", "玉蜀黍", "コーン");
        add(map, profile(75, new int[]{25, 52}, 550), "トマト", "とまと");
        add(map, profile(70, new int[]{25, 52}, 420), "ミニトマト", "みにとまと");
        add(map, profile(75, new int[]{25, 52}, 450), "なす", "茄子");
        add(map, profile(50, new int[]{20, 40}, 500), "きゅうり", "胡瓜");
        add(map, profile(75, new int[]{25, 58}, 260), "ピーマン", "ぴーまん", "パプリカ", "ぱぷりか");
        add(map, profile(105, new int[]{25, 50}, 220), "かぼちゃ", "南瓜");
        add(map, profile(50, new int[]{20, 40}, 300), "ズッキーニ", "ずっきーに");
        add(map, profile(60, new int[]{25, 52}, 150), "オクラ", "おくら");
        add(map, profile(90, new int[]{25, 50}, 300), "すいか", "西瓜");
        add(map, profile(88, new int[]{25, 50}, 250), "メロン", "めろん");
        add(map, profile(85, new int[]{35}, 100), "えだまめ", "枝豆");
        add(map, profile(140, new int[]{42}, 100), "落花生", "らっかせい", "ピーナッツ");
        add(map, profile(105, new int[]{25, 50}, 450), "キャベツ", "きゃべつ");
        add(map, profile(85, new int[]{25, 45}, 500), "はくさい", "白菜");
        add(map, profile(95, new int[]{25, 50}, 180), "ブロッコリー", "ぶろっこりー");
        add(map, profile(65, new int[]{25}, 300), "レタス", "れたす");
        add(map, profile(45, new int[]{22}, 150), "ほうれんそう", "ほうれん草", "菠菜");
        add(map, profile(33, new int[]{18}, 180), "こまつな", "小松菜");
        add(map, profile(150, new int[]{38, 80}, 250), "ねぎ", "葱", "長ねぎ", "長葱");
        add(map, profile(180, new int[]{38, 82}, 450), "たまねぎ", "玉ねぎ", "玉葱");
        add(map, profile(240, new int[]{52, 135}, 120), "にんにく", "大蒜");
        add(map, profile(180, new int[]{52, 90}, 250), "さといも", "里芋");
        add(map, profile(210, new int[]{70, 110}, 200), "しょうが", "生姜");

        return Map.copyOf(map);
    }

    private Profile profile(int harvestDays, int[] topdressingDays, double yieldKgPerAre) {
        return new Profile(harvestDays, topdressingDays, yieldKgPerAre, Map.of());
    }

    private Profile profile(int harvestDays, int[] topdressingDays, double yieldKgPerAre,
            Map<String, Integer> varietyHarvestDays) {
        return new Profile(harvestDays, topdressingDays, yieldKgPerAre, varietyHarvestDays);
    }

    private Map<String, Integer> varieties(Object... values) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(normalize(String.valueOf(values[i])), ((Number) values[i + 1]).intValue());
        }
        return Map.copyOf(map);
    }

    private void add(Map<String, Profile> map, Profile profile, String... names) {
        for (String name : names) map.put(normalize(name), profile);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.strip().toLowerCase(Locale.JAPAN)
                .replace(" ", "").replace("　", "").replace("・", "").replace("･", "")
                .replace("_", "").replace("-", "");
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record Plan(
            LocalDate suggestedHarvestDate,
            String topdressingGuide,
            Double expectedHarvestKg,
            Double yieldKgPerAre) {
    }

    private record Profile(
            int harvestDays,
            int[] topdressingDays,
            double yieldKgPerAre,
            Map<String, Integer> varietyHarvestDays) {
    }
}
