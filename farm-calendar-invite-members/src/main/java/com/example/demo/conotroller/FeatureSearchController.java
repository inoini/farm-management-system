package com.example.demo.conotroller;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FeatureSearchController {

    private static final List<FeatureItem> FEATURES = List.of(
        new FeatureItem("📅", "作業カレンダー", "予定・時間・担当者・作物・圃場を登録し、予定を検索できます。", "/calendar",
            "予定 カレンダー 作業 スケジュール 担当者 日程 今日 月"),
        new FeatureItem("🌱", "作物管理", "作物・品種・栽培情報・収穫予測などを管理します。", "/crop",
            "作物 品種 栽培 植付 収穫予定 生育 天気 予測"),
        new FeatureItem("🚜", "圃場管理", "圃場情報、住所、地図、面積などをまとめて管理します。", "/field",
            "圃場 畑 地図 住所 面積 土地 ピン"),
        new FeatureItem("◷", "作業履歴", "完了した作業や過去の作業記録を確認します。", "/work-history",
            "作業履歴 履歴 過去 完了 記録"),
        new FeatureItem("💰", "経営管理", "売上・経費・利益・予算と実績をまとめて確認します。", "/management",
            "経営 売上 経費 利益 収支 予算 実績 年間 月別 お金"),
        new FeatureItem("📦", "在庫管理", "肥料・農薬・資材などの在庫量と不足状況を管理します。", "/stock",
            "在庫 肥料 農薬 資材 残量 不足 入庫 出庫"),
        new FeatureItem("👥", "作業者管理", "作業者や担当者の情報を管理します。", "/workers",
            "作業者 担当者 メンバー 人員 担当"),
        new FeatureItem("🚚", "収穫・出荷管理", "収穫量・出荷・販売につながる記録を管理します。", "/harvests",
            "収穫 出荷 重量 kg 販売 品質 収量"),
        new FeatureItem("📊", "分析・レポート", "収穫量・売上・利益などの推移をグラフで確認します。", "/reports",
            "分析 レポート グラフ 推移 収穫量 売上 利益 月別 作物別"),
        new FeatureItem("🤖", "AI営農", "農作業や農場データについてAIに相談できます。", "/ai",
            "AI 相談 営農 質問 アドバイス 提案"),
        new FeatureItem("👤", "農場メンバー", "招待コードや農場メンバーの状況を管理します。", "/farm/members",
            "農場メンバー 招待コード 管理者 メンバー 共有 権限"),
        new FeatureItem("⚙", "設定", "画面レイアウトや表示設定などを変更します。", "/settings",
            "設定 レイアウト デザイン テーマ 表示 スマホ"),
        new FeatureItem("✉", "お問い合わせ", "システムに関する問い合わせを送信します。", "/contact",
            "問い合わせ 連絡 質問 サポート")
    );

    @GetMapping("/feature-search")
    public String featureSearch(
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            Model model) {

        String trimmed = query == null ? "" : query.trim();
        String normalized = normalize(trimmed);

        List<FeatureItem> results;
        if (normalized.isBlank()) {
            results = FEATURES;
        } else {
            List<String> terms = List.of(normalized.split("\\s+"));
            results = FEATURES.stream()
                .filter(item -> terms.stream().allMatch(term -> item.searchText().contains(term)))
                .sorted(Comparator
                    .comparingInt((FeatureItem item) -> score(item, terms))
                    .reversed()
                    .thenComparing(FeatureItem::getName))
                .toList();
        }

        model.addAttribute("query", trimmed);
        model.addAttribute("results", results);
        model.addAttribute("resultCount", results.size());
        model.addAttribute("totalCount", FEATURES.size());
        return "feature-search";
    }

    private static int score(FeatureItem item, List<String> terms) {
        String name = normalize(item.getName());
        String description = normalize(item.getDescription());
        String keywords = normalize(item.getKeywords());
        int score = 0;
        for (String term : terms) {
            if (name.contains(term)) {
                score += 5;
            }
            if (keywords.contains(term)) {
                score += 3;
            }
            if (description.contains(term)) {
                score += 1;
            }
        }
        return score;
    }

    private static String normalize(String value) {
        return value == null ? "" : value
            .toLowerCase(Locale.ROOT)
            .replace('　', ' ')
            .trim();
    }

    public static final class FeatureItem {
        private final String icon;
        private final String name;
        private final String description;
        private final String url;
        private final String keywords;

        public FeatureItem(String icon, String name, String description, String url, String keywords) {
            this.icon = icon;
            this.name = name;
            this.description = description;
            this.url = url;
            this.keywords = keywords;
        }

        public String getIcon() { return icon; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
        public String getKeywords() { return keywords; }

        private String searchText() {
            return normalize(name + " " + description + " " + keywords);
        }
    }
}
