package tn.tt.smartops.planning;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.absence.Absence;
import tn.tt.smartops.absence.AbsenceRepository;
import tn.tt.smartops.config.AppProperties;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.employee.EmployeeDtos.EmployeeSummary;
import tn.tt.smartops.employee.EmployeeRepository;
import tn.tt.smartops.employee.EmployeeStatus;
import tn.tt.smartops.equipment.Equipment;
import tn.tt.smartops.intervention.*;
import tn.tt.smartops.leave.LeaveRequest;
import tn.tt.smartops.leave.LeaveRequestRepository;
import tn.tt.smartops.planning.PlanningDtos.*;
import tn.tt.smartops.vehicle.Vehicle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calcule la disponibilité réelle à partir des congés approuvés, des absences et des
 * interventions déjà planifiées. La disponibilité n'est jamais un simple booléen stocké.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final LeaveRequestRepository leaveRepository;
    private final AbsenceRepository absenceRepository;
    private final InterventionAssignmentRepository assignmentRepository;
    private final InterventionRepository interventionRepository;
    private final EmployeeRepository employeeRepository;
    private final AppProperties props;

    @Transactional(readOnly = true)
    public List<Conflict> technicianConflicts(Employee tech, LocalDateTime start, LocalDateTime end,
                                              Long excludeInterventionId) {
        List<Conflict> conflicts = new ArrayList<>();
        if (tech.getStatus() != EmployeeStatus.ACTIF) {
            conflicts.add(new Conflict(ConflictType.INACTIF, "Employé " + tech.getStatus().name().toLowerCase(), null, null));
        }
        LocalDate d1 = start.toLocalDate();
        LocalDate d2 = end.minusNanos(1).toLocalDate();
        for (LeaveRequest l : leaveRepository.findApprovedOverlapping(tech.getId(), d1, d2)) {
            conflicts.add(new Conflict(ConflictType.CONGE,
                    "En congé " + l.getType().name().toLowerCase() + " du " + l.getStartDate() + " au " + l.getEndDate(),
                    l.getStartDate().atStartOfDay(), l.getEndDate().plusDays(1).atStartOfDay()));
        }
        for (Absence a : absenceRepository.findOverlapping(tech.getId(), d1, d2, null)) {
            conflicts.add(new Conflict(ConflictType.ABSENCE,
                    "Absent du " + a.getStartDate() + " au " + a.getEndDate(),
                    a.getStartDate().atStartOfDay(), a.getEndDate().plusDays(1).atStartOfDay()));
        }
        for (InterventionAssignment a : assignmentRepository.findTechnicianOverlaps(tech.getId(), start, end,
                InterventionStatus.BLOCKING, excludeInterventionId)) {
            Intervention i = a.getIntervention();
            conflicts.add(new Conflict(ConflictType.INTERVENTION,
                    "Déjà affecté à " + i.getReference() + " (" + i.getPlannedStart().toLocalTime() + " - "
                            + i.getPlannedEnd().toLocalTime() + ")",
                    i.getPlannedStart(), i.getPlannedEnd()));
        }
        return conflicts;
    }

    @Transactional(readOnly = true)
    public List<Conflict> equipmentConflicts(Equipment eq, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Conflict> conflicts = new ArrayList<>();
        if (!eq.getStatus().isBookable()) {
            conflicts.add(new Conflict(ConflictType.EQUIPEMENT, eq.getName() + " est " + eq.getStatus(), null, null));
        }
        interventionRepository.findEquipmentOverlaps(eq.getId(), start, end, InterventionStatus.BLOCKING, excludeId)
                .forEach(i -> conflicts.add(new Conflict(ConflictType.EQUIPEMENT,
                        eq.getName() + " réservé pour " + i.getReference(), i.getPlannedStart(), i.getPlannedEnd())));
        return conflicts;
    }

    @Transactional(readOnly = true)
    public List<Conflict> vehicleConflicts(Vehicle v, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Conflict> conflicts = new ArrayList<>();
        if (!v.getStatus().isBookable()) {
            conflicts.add(new Conflict(ConflictType.VEHICULE, v.getPlateNumber() + " est " + v.getStatus(), null, null));
        }
        interventionRepository.findVehicleOverlaps(v.getId(), start, end, InterventionStatus.BLOCKING, excludeId)
                .forEach(i -> conflicts.add(new Conflict(ConflictType.VEHICULE,
                        v.getPlateNumber() + " réservé pour " + i.getReference(), i.getPlannedStart(), i.getPlannedEnd())));
        return conflicts;
    }

    /**
     * Liste les techniciens actifs avec, pour chacun, l'adéquation à l'intervention et les conflits.
     * La décision reste au responsable technique : on informe, on ne choisit pas à sa place.
     */
    @Transactional(readOnly = true)
    public List<TechnicianCandidate> candidates(Intervention intervention) {
        Long teamId = intervention.getTeam() != null ? intervention.getTeam().getId() : null;
        return employeeRepository.findByTechnicianTrueAndStatusOrderByLastNameAsc(EmployeeStatus.ACTIF).stream()
                .map(tech -> {
                    boolean specialtyMatch = intervention.getRequiredSpecialty() == null
                            || intervention.getRequiredSpecialty() == tech.getSpecialty();
                    boolean sameTeam = teamId != null && tech.getTeam() != null && teamId.equals(tech.getTeam().getId());
                    boolean assigned = intervention.getAssignments().stream()
                            .anyMatch(a -> a.getTechnician().getId().equals(tech.getId()));
                    List<Conflict> conflicts = new ArrayList<>(technicianConflicts(tech,
                            intervention.getPlannedStart(), intervention.getPlannedEnd(), intervention.getId()));
                    if (!specialtyMatch) {
                        conflicts.add(0, new Conflict(ConflictType.SPECIALITE,
                                "Spécialité " + tech.getSpecialty() + " (requis : " + intervention.getRequiredSpecialty() + ")",
                                null, null));
                    }
                    return new TechnicianCandidate(EmployeeSummary.from(tech), specialtyMatch, sameTeam,
                            conflicts.isEmpty(), assigned, conflicts);
                })
                .sorted(Comparator.comparing(TechnicianCandidate::available).reversed()
                        .thenComparing(TechnicianCandidate::sameTeam, Comparator.reverseOrder())
                        .thenComparing(TechnicianCandidate::specialtyMatch, Comparator.reverseOrder()))
                .toList();
    }

    /** Découpe la journée de travail en créneaux DISPONIBLE / OCCUPE / CONGE / ABSENT. */
    @Transactional(readOnly = true)
    public List<AvailabilitySlot> dayTimeline(Employee tech, LocalDate day) {
        LocalTime workStart = props.planning().start();
        LocalTime workEnd = props.planning().end();
        LocalDateTime dayStart = day.atTime(workStart);
        LocalDateTime dayEnd = day.atTime(workEnd);

        if (!leaveRepository.findApprovedOverlapping(tech.getId(), day, day).isEmpty()) {
            return List.of(new AvailabilitySlot(dayStart, dayEnd, SlotState.CONGE, "En congé"));
        }
        if (!absenceRepository.findOverlapping(tech.getId(), day, day, null).isEmpty()) {
            return List.of(new AvailabilitySlot(dayStart, dayEnd, SlotState.ABSENT, "Absent"));
        }

        List<AvailabilitySlot> slots = new ArrayList<>();
        LocalDateTime cursor = dayStart;
        var busy = assignmentRepository.findTechnicianOverlaps(tech.getId(), dayStart, dayEnd,
                InterventionStatus.BLOCKING, null);
        for (InterventionAssignment a : busy) {
            Intervention i = a.getIntervention();
            LocalDateTime s = max(i.getPlannedStart(), dayStart);
            LocalDateTime e = min(i.getPlannedEnd(), dayEnd);
            if (s.isAfter(cursor)) {
                slots.add(new AvailabilitySlot(cursor, s, SlotState.DISPONIBLE, "Disponible"));
            }
            if (e.isAfter(cursor)) {
                slots.add(new AvailabilitySlot(max(s, cursor), e, SlotState.OCCUPE, i.getReference() + " · " + i.getTitle()));
                cursor = e;
            }
        }
        if (cursor.isBefore(dayEnd)) {
            slots.add(new AvailabilitySlot(cursor, dayEnd, SlotState.DISPONIBLE, "Disponible"));
        }
        return slots;
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) { return a.isAfter(b) ? a : b; }
    private static LocalDateTime min(LocalDateTime a, LocalDateTime b) { return a.isBefore(b) ? a : b; }
}
