package tn.tt.smartops.absence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class AbsenceDtos {
    private AbsenceDtos() {}

    public record AbsenceRequest(@NotNull Long employeeId, @NotNull AbsenceType type,
                                 @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                                 @Size(max = 500) String reason, boolean justified) {}

    public record AbsenceResponse(Long id, Long employeeId, String employeeName, String matricule, AbsenceType type,
                                  LocalDate startDate, LocalDate endDate, String reason, boolean justified,
                                  String recordedBy) {
        public static AbsenceResponse from(Absence a) {
            var e = a.getEmployee();
            return new AbsenceResponse(a.getId(), e.getId(), e.getFullName(), e.getMatricule(), a.getType(),
                    a.getStartDate(), a.getEndDate(), a.getReason(), a.isJustified(),
                    a.getRecordedBy() != null ? a.getRecordedBy().getEmail() : null);
        }
    }
}
