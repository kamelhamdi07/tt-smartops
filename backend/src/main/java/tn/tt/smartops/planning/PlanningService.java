package tn.tt.smartops.planning;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.absence.AbsenceRepository;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.DateRanges;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.equipment.EquipmentRepository;
import tn.tt.smartops.intervention.*;
import tn.tt.smartops.leave.LeaveRequestRepository;
import tn.tt.smartops.planning.PlanningDtos.*;
import tn.tt.smartops.security.CurrentUser;
import tn.tt.smartops.vehicle.VehicleRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanningService {

    private static final long MAX_RANGE_DAYS = 62;
    private static final List<AssetStatus> BOOKABLE = List.of(AssetStatus.DISPONIBLE, AssetStatus.AFFECTE);

    private final InterventionRepository interventionRepository;
    private final InterventionAssignmentRepository assignmentRepository;
    private final LeaveRequestRepository leaveRepository;
    private final AbsenceRepository absenceRepository;
    private final EquipmentRepository equipmentRepository;
    private final VehicleRepository vehicleRepository;
    private final AvailabilityService availabilityService;
    private final CurrentUser currentUser;

    /** Entrées du calendrier (vues jour / semaine / mois côté frontend). */
    @Transactional(readOnly = true)
    public List<PlanningEntry> entries(LocalDateTime from, LocalDateTime to, Long teamId, Long technicianId) {
        checkRange(from, to);
        List<Intervention> interventions = interventionRepository.findScheduledBetween(from, to, teamId);
        Map<Long, List<InterventionAssignment>> byIntervention = assignmentsOf(interventions);
        return interventions.stream()
                .filter(i -> technicianId == null || byIntervention.getOrDefault(i.getId(), List.of()).stream()
                        .anyMatch(a -> a.getTechnician().getId().equals(technicianId)))
                .map(i -> new PlanningEntry(i.getId(), i.getReference(), i.getTitle(), i.getPriority(), i.getStatus(),
                        i.getLocation(), i.getPlannedStart(), i.getPlannedEnd(),
                        i.getTeam() != null ? i.getTeam().getName() : null,
                        byIntervention.getOrDefault(i.getId(), List.of()).stream()
                                .map(a -> a.getTechnician().getFullName()).toList(),
                        i.getVehicle() != null ? i.getVehicle().getPlateNumber() : null,
                        i.getEquipment() != null ? i.getEquipment().getName() : null))
                .toList();
    }

    /** Planning personnel du technicien connecté. */
    @Transactional(readOnly = true)
    public List<PlanningEntry> myEntries(LocalDateTime from, LocalDateTime to) {
        return entries(from, to, null, currentUser.requireEmployeeId());
    }

    /**
     * Analyse le planning existant et remonte les incohérences : double affectation,
     * intervention pendant un congé ou une absence, double réservation d'équipement ou de véhicule.
     */
    @Transactional(readOnly = true)
    public List<PlanningConflict> detectConflicts(LocalDateTime from, LocalDateTime to) {
        checkRange(from, to);
        List<PlanningConflict> result = new ArrayList<>();
        var assignments = assignmentRepository.findActiveBetween(from, to, InterventionStatus.BLOCKING);

        // 1. Même technicien sur deux interventions simultanées
        assignments.stream().collect(Collectors.groupingBy(a -> a.getTechnician().getId()))
                .values().forEach(list -> pairwise(list, InterventionAssignment::getIntervention, (a, b) ->
                        result.add(new PlanningConflict(ConflictType.INTERVENTION,
                                a.getTechnician().getFullName() + " est affecté simultanément à "
                                        + a.getIntervention().getReference() + " et " + b.getIntervention().getReference(),
                                List.of(a.getIntervention().getId(), b.getIntervention().getId())))));

        // 2. Intervention pendant un congé approuvé ou une absence
        var leaves = leaveRepository.findApprovedBetween(from.toLocalDate(), to.toLocalDate()).stream()
                .collect(Collectors.groupingBy(l -> l.getEmployee().getId()));
        var absences = absenceRepository.findBetween(from.toLocalDate(), to.toLocalDate()).stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));
        for (InterventionAssignment a : assignments) {
            Intervention i = a.getIntervention();
            Employee t = a.getTechnician();
            var start = i.getPlannedStart().toLocalDate();
            var end = i.getPlannedEnd().minusNanos(1).toLocalDate();
            leaves.getOrDefault(t.getId(), List.of()).stream()
                    .filter(l -> !l.getStartDate().isAfter(end) && !l.getEndDate().isBefore(start))
                    .findFirst().ifPresent(l -> result.add(new PlanningConflict(ConflictType.CONGE,
                            t.getFullName() + " est en congé pendant " + i.getReference(), List.of(i.getId()))));
            absences.getOrDefault(t.getId(), List.of()).stream()
                    .filter(ab -> !ab.getStartDate().isAfter(end) && !ab.getEndDate().isBefore(start))
                    .findFirst().ifPresent(ab -> result.add(new PlanningConflict(ConflictType.ABSENCE,
                            t.getFullName() + " est absent pendant " + i.getReference(), List.of(i.getId()))));
        }

        // 3. Équipements et véhicules réservés deux fois
        var active = interventionRepository.findScheduledBetween(from, to, null).stream()
                .filter(i -> InterventionStatus.BLOCKING.contains(i.getStatus())).toList();
        active.stream().filter(i -> i.getEquipment() != null)
                .collect(Collectors.groupingBy(i -> i.getEquipment().getId())).values()
                .forEach(list -> pairwise(list, Function.identity(), (a, b) -> result.add(new PlanningConflict(
                        ConflictType.EQUIPEMENT, a.getEquipment().getName() + " est réservé pour "
                        + a.getReference() + " et " + b.getReference(), List.of(a.getId(), b.getId())))));
        active.stream().filter(i -> i.getVehicle() != null)
                .collect(Collectors.groupingBy(i -> i.getVehicle().getId())).values()
                .forEach(list -> pairwise(list, Function.identity(), (a, b) -> result.add(new PlanningConflict(
                        ConflictType.VEHICULE, "Le véhicule " + a.getVehicle().getPlateNumber() + " est réservé pour "
                        + a.getReference() + " et " + b.getReference(), List.of(a.getId(), b.getId())))));
        return result;
    }

    /** Équipements et véhicules libres sur un créneau, pour alimenter les listes du formulaire. */
    @Transactional(readOnly = true)
    public AvailableResources availableResources(LocalDateTime start, LocalDateTime end, Long excludeInterventionId) {
        DateRanges.requireOrdered(start, end);
        var equipment = equipmentRepository.findByStatusInOrderByNameAsc(BOOKABLE).stream()
                .filter(e -> availabilityService.equipmentConflicts(e, start, end, excludeInterventionId).isEmpty())
                .map(e -> new AvailableResource(e.getId(), e.getReference() + " · " + e.getName())).toList();
        var vehicles = vehicleRepository.findByStatusInOrderByPlateNumberAsc(BOOKABLE).stream()
                .filter(v -> availabilityService.vehicleConflicts(v, start, end, excludeInterventionId).isEmpty())
                .map(v -> new AvailableResource(v.getId(), v.getPlateNumber() + " · " + v.getBrandModel())).toList();
        return new AvailableResources(equipment, vehicles);
    }

    private Map<Long, List<InterventionAssignment>> assignmentsOf(List<Intervention> interventions) {
        if (interventions.isEmpty()) return Map.of();
        return assignmentRepository.findByInterventionIds(interventions.stream().map(Intervention::getId).toList())
                .stream().collect(Collectors.groupingBy(a -> a.getIntervention().getId()));
    }

    private static <T> void pairwise(List<T> items, Function<T, Intervention> toIntervention,
                                     java.util.function.BiConsumer<T, T> onOverlap) {
        for (int x = 0; x < items.size(); x++) {
            for (int y = x + 1; y < items.size(); y++) {
                Intervention a = toIntervention.apply(items.get(x));
                Intervention b = toIntervention.apply(items.get(y));
                if (a.getPlannedStart().isBefore(b.getPlannedEnd()) && b.getPlannedStart().isBefore(a.getPlannedEnd())) {
                    onOverlap.accept(items.get(x), items.get(y));
                }
            }
        }
    }

    private static void checkRange(LocalDateTime from, LocalDateTime to) {
        DateRanges.requireOrdered(from, to);
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BusinessException("La période demandée ne doit pas dépasser " + MAX_RANGE_DAYS + " jours.");
        }
    }
}
