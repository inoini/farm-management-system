package com.example.demo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component("moneyFormatter")
public class JapaneseMoneyFormatter {

    private static final long MAN = 10_000L;
    private static final long OKU = 100_000_000L;
    private static final long CHO = 1_000_000_000_000L;

    public String format(Number value) {
        if (value == null) {
            return "0円";
        }

        long amount = BigDecimal.valueOf(value.doubleValue())
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        if (amount == 0) {
            return "0円";
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
        return result.append("円").toString();
    }

    private String withComma(long value) {
        return NumberFormat.getIntegerInstance(Locale.JAPAN).format(value);
    }
}
