package tn.tt.smartops.common;

import org.junit.jupiter.api.Test;
import tn.tt.smartops.common.exception.BusinessException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateRangesTest {

    @Test
    void countsOnlyWorkingDays() {
        // lundi 9 novembre 2026 -> dimanche 15 novembre 2026
        assertEquals(5, DateRanges.workingDays(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15)));
    }

    @Test
    void rejectsInvertedPeriod() {
        assertThrows(BusinessException.class,
                () -> DateRanges.requireOrdered(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9)));
    }
}
