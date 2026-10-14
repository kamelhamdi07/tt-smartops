package tn.tt.smartops.common;

import tn.tt.smartops.common.exception.BusinessException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Utilitaires de période partagés par les modules congés, absences et planning. */
public final class DateRanges {

    private DateRanges() {}

    public static void requireOrdered(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new BusinessException("La date de fin doit être postérieure ou égale à la date de début.");
        }
    }

    public static void requireOrdered(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessException("L'heure de fin doit être postérieure à l'heure de début.");
        }
    }

    /** Jours ouvrés (lundi-vendredi) entre deux dates incluses. */
    public static int workingDays(LocalDate start, LocalDate end) {
        int days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days++;
            }
        }
        return days;
    }
}
