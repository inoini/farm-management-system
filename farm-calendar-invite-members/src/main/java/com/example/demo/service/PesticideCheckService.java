package com.example.demo.service;

import com.example.demo.entity.Stock;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Compares proposed use with farmer-entered inventory values.
 * This is NOT a registration database, legal judgement or approval to spray.
 */
@Service
public class PesticideCheckService {
    public record Check(String title, String detail, String level) {}
    public record Report(List<Check> checks, boolean hasWarnings) {}

    private static final Pattern RATE =
            Pattern.compile("^(\\d+)(?:\\s*[-~]\\s*(\\d+))?\\s*倍?$");

    public Report check(Stock stock, String crop, String dilution,
                        LocalDate applicationDate, LocalDate harvestDate) {
        List<Check> checks = new ArrayList<>();

        if (blank(crop) || blank(stock.getTargetCrop())) {
            checks.add(new Check("適用作物", "作物または在庫に登録した対象作物が未設定です。農薬ラベルで確認してください。", "unknown"));
        } else {
            boolean matches = List.of(stock.getTargetCrop().split("[、,，;/／\\s]+"))
                    .stream().map(String::strip).anyMatch(c -> c.equalsIgnoreCase(crop.strip()));
            checks.add(new Check("適用作物",
                    matches ? "登録した対象作物名と一致しています。正式な適用作物名はラベルで再確認してください。"
                            : "登録した対象作物と一致しません。使用前に登録情報を確認してください。",
                    matches ? "ok" : "warning"));
        }

        long[] labelRange = parseRate(stock.getDilutionRate());
        long[] actual = parseRate(dilution);
        if (labelRange == null || actual == null || actual[0] != actual[1]) {
            checks.add(new Check("希釈倍率",
                    "在庫の希釈倍率と今回の希釈倍率を数字で入力してください（例：1000倍、登録条件は1000～2000倍）。",
                    "unknown"));
        } else {
            boolean inRange = actual[0] >= labelRange[0] && actual[0] <= labelRange[1];
            checks.add(new Check("希釈倍率",
                    inRange ? "登録した希釈倍率の範囲内です。"
                            : "入力した希釈倍率が登録値の範囲外です。",
                    inRange ? "ok" : "warning"));
        }

        Integer max = stock.getMaxUsageCount();
        Integer used = stock.getUsedCount();
        if (max == null || used == null || max < 0 || used < 0) {
            checks.add(new Check("使用回数",
                    "登録された最大使用回数または累計使用回数が不明です。使用履歴とラベルを照合してください。", "unknown"));
        } else {
            boolean allowed = used < max;
            checks.add(new Check("使用回数",
                    allowed ? "登録値では今回の使用後 " + (used + 1) + "回 / 最大" + max + "回です。"
                            : "登録された最大使用回数に達しています。散布しないで登録情報を確認してください。",
                    allowed ? "ok" : "warning"));
        }

        Integer beforeHarvest = stock.getPreHarvestDays();
        if (beforeHarvest == null || beforeHarvest < 0
                || applicationDate == null || harvestDate == null) {
            checks.add(new Check("収穫前の使用時期",
                    "使用予定日、収穫予定日、または収穫前日数が未設定です。ラベルに照らして確認してください。", "unknown"));
        } else {
            long days = ChronoUnit.DAYS.between(applicationDate, harvestDate);
            boolean allowed = days >= beforeHarvest;
            checks.add(new Check("収穫前の使用時期",
                    allowed ? "収穫まで" + days + "日あり、在庫に登録した" + beforeHarvest + "日以上を満たします。"
                            : "収穫まで" + days + "日で、登録した収穫前" + beforeHarvest + "日の条件を満たしません。",
                    allowed ? "ok" : "warning"));
        }

        checks.add(new Check("病害虫・使用方法・有効成分の総使用回数",
                "この画面では未照合です。同じ有効成分を含む他剤も含め、最新の登録情報と農薬ラベルを必ず確認してください。", "unknown"));
        return new Report(List.copyOf(checks), checks.stream().anyMatch(c -> "warning".equals(c.level())));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** Parse farmer-entered "1000倍" or "1000～2000倍". Reject ambiguous text. */
    static long[] parseRate(String raw) {
        if (blank(raw)) return null;
        String normalized = Normalizer.normalize(raw.strip(), Normalizer.Form.NFKC)
                .replace("〜", "-").replace("～", "-").replace("~", "-");
        Matcher m = RATE.matcher(normalized);
        if (!m.matches()) return null;
        try {
            long min = Long.parseLong(m.group(1));
            long max = m.group(2) == null ? min : Long.parseLong(m.group(2));
            return min > 0 && max >= min ? new long[]{min, max} : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
