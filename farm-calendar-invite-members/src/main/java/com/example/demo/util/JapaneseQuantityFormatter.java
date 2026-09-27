package com.example.demo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * 重量などの数量を、日本で読みやすい「万・億・兆」単位で表示する。
 * 例: 12000 -> 1万2,000kg
 */
@Component("quantityFormatter")
public class JapaneseQuantityFormatter {

    private static final long MAN = 10_000L;
    private static final long OKU = 100_000_000L;
    private static final long CHO = 1_000_000_000_000L;

    public String formatKg(Number value) {
        if (value == null) {
            return "0kg";
        }

        long amount = BigDecimal.valueOf(value.doubleValue())
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        if (amount == 0) {
            return "0kg";
        }

        boolean negative = amount < 0;
        long absolute = Math.abs(amount);
        StringBuilder result = new StringBuilder();

        long cho = absolute / CHO;
        absolute %= CHO;
        long oku = absolute / OKU;
        absolute %= OKU;
        long man = absolute / MAN;
        long remainder = absolute % MAN;

        if (cho > 0) {
            result.append(withComma(cho)).append("兆");
        }
        if (oku > 0) {
            result.append(withComma(oku)).append("億");
        }
        if (man > 0) {
            result.append(withComma(man)).append("万");
        }
        if (remainder > 0) {
            result.append(withComma(remainder));
        }

        if (negative) {
            result.insert(0, "−");
        }
        return result.append("kg").toString();
    }

    private String withComma(long value) {
        return NumberFormat.getIntegerInstance(Locale.JAPAN).format(value);
    }
}
