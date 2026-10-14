package tn.tt.smartops.intervention;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.tt.smartops.employee.Specialty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class InterventionDtos {
    private InterventionDtos() {}

    public record InterventionRequest(@NotBlank @Size(max = 200) String title,
                                      @Size(max = 5000) String description,
                                      @NotNull InterventionType type,
                                      @NotNull InterventionPriority priority,
                                      @NotBlank @Size(max = 255) String location,
                                      Specialty requiredSpecialty,
                                      LocalDateTime plannedStart,
                                      LocalDateTime plannedEnd,
                                      LocalDate dueDate,
                                      Long teamId,
                                      Long equipmentId,
                                      Long vehicleId) {}

    public record ScheduleRequest(@NotNull LocalDateTime plannedStart, @NotNull LocalDateTime plannedEnd) {}

    public record AssignRequest(@NotNull Long technicianId) {}

    public record ResourcesRequest(Long equipmentId, Long vehicleId) {}

    public record CommentRequest(@Size(max = 500) String comment) {}

    public record AssignedTechnician(Long id, String fullName, Specialty specialty, LocalDateTime assignedAt,
                                     LocalDateTime acknowledgedAt) {}

    public record InterventionResponse(Long id, String reference, String title, String description,
                                       InterventionType type, InterventionPriority priority, String location,
                                       Specialty requiredSpecialty, LocalDateTime plannedStart,
                                       LocalDateTime plannedEnd, LocalDate dueDate, InterventionStatus status,
                                       boolean late, Long teamId, String teamName, Long equipmentId,
                                       String equipmentName, Long vehicleId, String vehiclePlate, String createdBy,
                                       LocalDateTime createdAt, LocalDateTime actualStart, LocalDateTime actualEnd,
                                       String cancelReason, List<AssignedTechnician> technicians) {}

    /** Ligne de liste : on évite de charger les affectations pour chaque intervention. */
    public record InterventionListItem(Long id, String reference, String title, InterventionType type,
                                       InterventionPriority priority, String location, LocalDateTime plannedStart,
                                       LocalDateTime plannedEnd, InterventionStatus status, boolean late,
                                       String teamName) {
        public static InterventionListItem from(Intervention i) {
            return new InterventionListItem(i.getId(), i.getReference(), i.getTitle(), i.getType(), i.getPriority(),
                    i.getLocation(), i.getPlannedStart(), i.getPlannedEnd(), i.getStatus(), i.isLate(),
                    i.getTeam() != null ? i.getTeam().getName() : null);
        }
    }

    public record StatusHistoryResponse(InterventionStatus fromStatus, InterventionStatus toStatus,
                                        String changedBy, LocalDateTime changedAt, String comment) {}
}
