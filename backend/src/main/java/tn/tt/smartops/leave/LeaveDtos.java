package tn.tt.smartops.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class LeaveDtos {
    private LeaveDtos() {}

    public record LeaveCreateRequest(@NotNull LeaveType type,
                                     @NotNull LocalDate startDate,
                                     @NotNull LocalDate endDate,
                                     @Size(max = 500) String comment) {}

    public record LeaveDecisionRequest(@NotNull Boolean approve, @Size(max = 500) String comment) {}

    public record LeaveResponse(Long id, Long employeeId, String employeeName, String matricule, LeaveType type,
                                LocalDate startDate, LocalDate endDate, int workingDays, String comment,
                                LeaveStatus status, String decisionComment, LocalDateTime createdAt,
                                LocalDateTime processedAt, String processedBy) {
        public static LeaveResponse from(LeaveRequest l) {
            var e = l.getEmployee();
            return new LeaveResponse(l.getId(), e.getId(), e.getFullName(), e.getMatricule(), l.getType(),
                    l.getStartDate(), l.getEndDate(), l.getWorkingDays(), l.getComment(), l.getStatus(),
                    l.getDecisionComment(), l.getCreatedAt(), l.getProcessedAt(),
                    l.getProcessedBy() != null ? l.getProcessedBy().getEmail() : null);
        }
    }
}
