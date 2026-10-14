package com.example.demo.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.demo.entity.Field;

/**
 * Display labels for registered fields. Distinct fields with identical names
 * must remain distinguishable by their persistent ID.
 */
public final class FieldLabels {
    private FieldLabels() {}

    public static Map<Long, String> from(List<Field> fields) {
        Map<String, Long> counts = fields.stream()
                .collect(Collectors.groupingBy(f -> cleanName(f.getName()), Collectors.counting()));
        Map<Long, String> labels = new LinkedHashMap<>();
        for (Field field : fields) {
            String name = cleanName(field.getName());
            labels.put(field.getId(), counts.getOrDefault(name, 0L) > 1L
                    ? name + "（ID:" + field.getId() + "）" : name);
        }
        return labels;
    }

    public static String label(Map<Long, String> labels, Long fieldId) {
        if (fieldId == null) return "圃場未設定";
        return labels.getOrDefault(fieldId, "登録外の圃場（ID:" + fieldId + "）");
    }

    private static String cleanName(String name) {
        return name == null || name.isBlank() ? "名称未設定" : name.strip();
    }
}
