package com.example.demo.util;

import java.text.Normalizer;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.example.demo.entity.Schedule;

public final class ScheduleSearch {
    private ScheduleSearch() {}

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).strip();
    }

    public static List<Schedule> find(List<Schedule> schedules, String query, YearMonth month) {
        String normalized = normalize(query);
        if (normalized.isBlank()) return List.of();
        String[] words = normalized.split("\\s+");
        return schedules.stream()
                .filter(s -> month == null || (s.getDate() != null
                        && s.getDate().startsWith(month.toString() + "-")))
                .filter(s -> {
                    String text = Stream.of(s.getSchedule(), s.getCropName(), s.getFieldName(),
                            s.getUserName(), s.getWorkType(), s.getStatus(), s.getMemo(), s.getDate())
                            .map(ScheduleSearch::normalize).collect(Collectors.joining("\n"));
                    return Arrays.stream(words).allMatch(text::contains);
                })
                .sorted(Comparator.comparing(Schedule::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Schedule::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Schedule::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
