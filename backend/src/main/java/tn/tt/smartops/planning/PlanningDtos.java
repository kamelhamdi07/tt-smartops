package tn.tt.smartops.planning;

import tn.tt.smartops.employee.EmployeeDtos.EmployeeSummary;
import tn.tt.smartops.intervention.InterventionPriority;
import tn.tt.smartops.intervention.InterventionStatus;

import java.time.LocalDateTime;
import java.util.List;

public final class PlanningDtos {
    private PlanningDtos() {}

    public enum ConflictType { INACTIF, SPECIALITE, CONGE, ABSENCE, INTERVENTION, EQUIPEMENT, VEHICULE }

    /** Raison pour laquelle une ressource ne peut pas être utilisée sur un créneau. */
    public record Conflict(ConflictType type, String message, LocalDateTime from, LocalDateTime to) {}

    public record TechnicianCandidate(EmployeeSummary technician, boolean specialtyMatch, boolean sameTeam,
                                      boolean available, boolean alreadyAssigned, List<Conflict> conflicts) {}

    public enum SlotState { DISPONIBLE, OCCUPE, CONGE, ABSENT }

    public record AvailabilitySlot(LocalDateTime start, LocalDateTime end, SlotState state, String label) {}

    public record PlanningEntry(Long interventionId, String reference, String title, InterventionPriority priority,
                                InterventionStatus status, String location, LocalDateTime start, LocalDateTime end,
                                String teamName, List<String> technicians, String vehicle, String equipment) {}

    public record PlanningConflict(ConflictType type, String message, List<Long> interventionIds) {}

    public record AvailableResource(Long id, String label) {}

    public record AvailableResources(List<AvailableResource> equipment, List<AvailableResource> vehicles) {}
}
