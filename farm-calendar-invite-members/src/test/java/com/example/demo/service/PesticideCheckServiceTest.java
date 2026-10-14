package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import com.example.demo.entity.Stock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PesticideCheckServiceTest {
    private final PesticideCheckService service = new PesticideCheckService();

    @Test
    void comparesInputButAlwaysMarksUnverifiedConditions() {
        Stock s = stock();
        var report = service.check(s, "さつまいも", "1500倍",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));
        assertFalse(report.hasWarnings());
        assertEquals(5, report.checks().size());
        assertEquals("unknown", report.checks().get(4).level());
        assertEquals("ok", report.checks().get(0).level());
    }

    @Test
    void warnsOnExhaustedUsesAndInsufficientPreHarvestDays() {
        Stock s = stock();
        s.setUsedCount(3);
        var report = service.check(s, "じゃがいも", "3000倍",
                LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 15));
        assertTrue(report.hasWarnings());
        assertEquals("warning", report.checks().get(0).level());
        assertEquals("warning", report.checks().get(1).level());
        assertEquals("warning", report.checks().get(2).level());
        assertEquals("warning", report.checks().get(3).level());
    }

    @Test
    void doesNotAssumeUnfilledInventoryRestrictionsAreSafe() {
        var report = service.check(new Stock(), "さつまいも", "1000倍", null, null);
        assertTrue(report.checks().stream().allMatch(c -> "unknown".equals(c.level())));
    }

    @Test
    void parsesFullwidthAndRangesButNotFreeform() {
        assertArrayEquals(new long[]{1000, 2000}, PesticideCheckService.parseRate("１０００～２０００倍"));
        assertArrayEquals(new long[]{1000, 1000}, PesticideCheckService.parseRate("1000倍"));
        assertNull(PesticideCheckService.parseRate("約1000倍"));
    }

    private static Stock stock() {
        Stock s = new Stock();
        s.setCategory("農薬");
        s.setTargetCrop("さつまいも、にんじん");
        s.setDilutionRate("1000～2000倍");
        s.setMaxUsageCount(3);
        s.setUsedCount(1);
        s.setPreHarvestDays(7);
        return s;
    }
}
