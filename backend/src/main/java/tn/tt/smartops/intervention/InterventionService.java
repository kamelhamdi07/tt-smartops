package tn.tt.smartops.intervention;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.auth.Role;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.DateRanges;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.employee.EmployeeService;
import tn.tt.smartops.equipment.Equipment;
import tn.tt.smartops.equipment.EquipmentService;
import tn.tt.smartops.intervention.InterventionDtos.*;
import tn.tt.smartops.notification.NotificationService;
import tn.tt.smartops.notification.NotificationType;
import tn.tt.smartops.planning.AvailabilityService;
import tn.tt.smartops.planning.PlanningDtos.Conflict;
import tn.tt.smartops.planning.PlanningDtos.TechnicianCandidate;
import tn.tt.smartops.security.CurrentUser;
import tn.tt.smartops.team.TeamService;
import tn.tt.smartops.vehicle.Vehicle;
import tn.tt.smartops.vehicle.VehicleService;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterventionService {

    private final InterventionRepository repository;
    private final InterventionAssignmentRepository assignmentRepository;
    private final InterventionStatusHistoryRepository historyRepository;
    private final AvailabilityService availability;
    private final EmployeeService employeeService;
    private final TeamService teamService;
    private final EquipmentService equipmentService;
    private final VehicleService vehicleService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final NotificationService notifications;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    // ---------------------------------------------------------------- lecture

    @Transactional(readOnly = true)
    public PageResponse<InterventionListItem> search(String q, InterventionStatus status, InterventionPriority priority,
                                                     Long teamId, Long technicianId, LocalDateTime from,
                                                     LocalDateTime to, Pageable pageable) {
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(repository.search(query, status, priority, teamId, technicianId, from, to, pageable)
                .map(InterventionListItem::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<InterventionListItem> mine(InterventionStatus status, Pageable pageable) {
        return search(null, status, null, null, currentUser.requireEmployeeId(), null, null, pageable);
    }

    @Transactional(readOnly = true)
    public InterventionResponse get(Long id) {
        return toResponse(loadVisible(id));
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> history(Long id) {
        loadVisible(id);
        return historyRepository.findByInterventionIdOrderByChangedAtAsc(id).stream()
                .map(h -> new StatusHistoryResponse(h.getFromStatus(), h.getToStatus(), h.getChangedBy(),
                        h.getChangedAt(), h.getComment()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TechnicianCandidate> candidates(Long id) {
        Intervention i = load(id);
        requireScheduled(i);
        return availability.candidates(i);
    }

    // ---------------------------------------------------------------- création / planification

    @Transactional
    public InterventionResponse create(InterventionRequest req) {
        Intervention i = new Intervention();
        i.setReference(String.format("INT-%d-%05d", Year.now().getValue(), repository.nextReferenceNumber()));
        i.setCreatedBy(userRepository.getReferenceById(currentUser.get().getId()));
        applyDescriptiveFields(i, req);
        applySchedule(i, req.plannedStart(), req.plannedEnd());
        applyResources(i, req.equipmentId(), req.vehicleId());
        repository.save(i);
        recordHistory(i, null, InterventionStatus.NOUVELLE, "Création");
        if (i.isScheduled()) {
            changeStatus(i, InterventionStatus.PLANIFIEE, "Planifiée à la création");
        }
        audit.log("INTERVENTION_CREATED", "Intervention", i.getId(), i.getReference());
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse update(Long id, InterventionRequest req) {
        Intervention i = load(id);
        requireEditable(i);
        applyDescriptiveFields(i, req);
        boolean datesChanged = applySchedule(i, req.plannedStart(), req.plannedEnd());
        applyResources(i, req.equipmentId(), req.vehicleId());
        afterScheduleChange(i, datesChanged);
        audit.log("INTERVENTION_UPDATED", "Intervention", id, i.getReference());
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse schedule(Long id, ScheduleRequest req) {
        Intervention i = load(id);
        requireEditable(i);
        boolean changed = applySchedule(i, req.plannedStart(), req.plannedEnd());
        // les ressources déjà réservées doivent rester libres sur le nouveau créneau
        applyResources(i, i.getEquipment() != null ? i.getEquipment().getId() : null,
                i.getVehicle() != null ? i.getVehicle().getId() : null);
        afterScheduleChange(i, changed);
        audit.log("INTERVENTION_SCHEDULED", "Intervention", id, req.plannedStart() + " -> " + req.plannedEnd());
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse setResources(Long id, ResourcesRequest req) {
        Intervention i = load(id);
        requireEditable(i);
        applyResources(i, req.equipmentId(), req.vehicleId());
        audit.log("INTERVENTION_RESOURCES_SET", "Intervention", id,
                "équipement=" + req.equipmentId() + ", véhicule=" + req.vehicleId());
        return toResponse(i);
    }

    // ---------------------------------------------------------------- affectation

    @Transactional
    public InterventionResponse assign(Long id, Long technicianId) {
        Intervention i = load(id);
        if (i.getStatus() != InterventionStatus.PLANIFIEE && i.getStatus() != InterventionStatus.AFFECTEE) {
            throw new BusinessException("L'intervention doit être planifiée avant toute affectation.");
        }
        requireScheduled(i);
        Employee tech = employeeService.load(technicianId);
        if (!tech.isTechnician()) {
            throw new BusinessException(tech.getFullName() + " n'est pas technicien.");
        }
        if (assignmentRepository.existsByInterventionIdAndTechnicianId(id, technicianId)) {
            throw new ConflictException(tech.getFullName() + " est déjà affecté à cette intervention.");
        }
        List<String> problems = new ArrayList<>();
        if (i.getRequiredSpecialty() != null && i.getRequiredSpecialty() != tech.getSpecialty()) {
            problems.add("Spécialité " + tech.getSpecialty() + " alors que " + i.getRequiredSpecialty() + " est requise.");
        }
        availability.technicianConflicts(tech, i.getPlannedStart(), i.getPlannedEnd(), id)
                .forEach(c -> problems.add(c.message()));
        if (!problems.isEmpty()) {
            throw new ConflictException("Affectation impossible pour " + tech.getFullName() + ".", problems);
        }

        InterventionAssignment a = new InterventionAssignment();
        a.setIntervention(i);
        a.setTechnician(tech);
        a.setAssignedBy(userRepository.getReferenceById(currentUser.get().getId()));
        i.getAssignments().add(a);
        if (i.getStatus() == InterventionStatus.PLANIFIEE) {
            changeStatus(i, InterventionStatus.AFFECTEE, "Affectation de " + tech.getFullName());
        }
        audit.log("TECHNICIAN_ASSIGNED", "Intervention", id, tech.getMatricule());
        notifications.notifyEmployee(tech.getId(), NotificationType.INTERVENTION_AFFECTEE,
                "Nouvelle intervention " + i.getReference(),
                i.getTitle() + " · " + i.getLocation() + " · " + i.getPlannedStart().toLocalDate() + " "
                        + i.getPlannedStart().toLocalTime() + "-" + i.getPlannedEnd().toLocalTime(),
                "/interventions/" + id);
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse unassign(Long id, Long technicianId) {
        Intervention i = load(id);
        requireEditable(i);
        boolean removed = i.getAssignments().removeIf(a -> a.getTechnician().getId().equals(technicianId));
        if (!removed) {
            throw new NotFoundException("Affectation", technicianId);
        }
        if (i.getAssignments().isEmpty() && i.getStatus() == InterventionStatus.AFFECTEE) {
            changeStatus(i, InterventionStatus.PLANIFIEE, "Plus aucun technicien affecté");
        }
        audit.log("TECHNICIAN_UNASSIGNED", "Intervention", id, "technicien " + technicianId);
        notifications.notifyEmployee(technicianId, NotificationType.INTERVENTION_MODIFIEE,
                "Désaffectation " + i.getReference(), "Vous n'êtes plus affecté à " + i.getTitle() + ".", null);
        return toResponse(i);
    }

    // ---------------------------------------------------------------- exécution (technicien)

    @Transactional
    public InterventionResponse acknowledge(Long id) {
        Intervention i = load(id);
        InterventionAssignment a = requireMyAssignment(i);
        if (a.getAcknowledgedAt() == null) {
            a.setAcknowledgedAt(LocalDateTime.now());
            audit.log("INTERVENTION_ACKNOWLEDGED", "Intervention", id, null);
        }
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse start(Long id) {
        Intervention i = load(id);
        requireMyAssignment(i);
        changeStatus(i, InterventionStatus.EN_COURS, null);
        if (i.getActualStart() == null) i.setActualStart(LocalDateTime.now());
        setAssetsStatus(i, AssetStatus.AFFECTE);
        audit.log("INTERVENTION_STARTED", "Intervention", id, null);
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse suspend(Long id, String comment) {
        Intervention i = load(id);
        requireAssignedOrManager(i);
        changeStatus(i, InterventionStatus.SUSPENDUE, comment);
        audit.log("INTERVENTION_SUSPENDED", "Intervention", id, comment);
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse resume(Long id) {
        Intervention i = load(id);
        requireAssignedOrManager(i);
        changeStatus(i, InterventionStatus.EN_COURS, "Reprise");
        audit.log("INTERVENTION_RESUMED", "Intervention", id, null);
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse complete(Long id) {
        Intervention i = load(id);
        requireMyAssignment(i);
        changeStatus(i, InterventionStatus.TERMINEE, null);
        i.setActualEnd(LocalDateTime.now());
        setAssetsStatus(i, AssetStatus.DISPONIBLE);
        audit.log("INTERVENTION_COMPLETED", "Intervention", id, null);
        return toResponse(i);
    }

    @Transactional
    public InterventionResponse cancel(Long id, String reason) {
        Intervention i = load(id);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Le motif d'annulation est obligatoire.");
        }
        changeStatus(i, InterventionStatus.ANNULEE, reason);
        i.setCancelReason(reason);
        setAssetsStatus(i, AssetStatus.DISPONIBLE);
        audit.log("INTERVENTION_CANCELLED", "Intervention", id, reason);
        i.getAssignments().forEach(a -> notifications.notifyEmployee(a.getTechnician().getId(),
                NotificationType.INTERVENTION_ANNULEE, "Intervention annulée " + i.getReference(),
                i.getTitle() + " · motif : " + reason, "/interventions/" + id));
        return toResponse(i);
    }

    /** Appelé par le module rapport lorsque le rapport est validé : clôture de l'intervention. */
    @Transactional
    public void close(Intervention i, String comment) {
        changeStatus(i, InterventionStatus.VALIDEE, comment);
        audit.log("INTERVENTION_CLOSED", "Intervention", i.getId(), i.getReference());
    }

    // ---------------------------------------------------------------- règles internes

    public Intervention load(Long id) {
        return repository.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("Intervention", id));
    }

    /** Un technicien ne voit que les interventions qui lui sont affectées. */
    public Intervention loadVisible(Long id) {
        Intervention i = load(id);
        if (currentUser.hasRole(Role.TECHNICIEN) && findMyAssignment(i) == null) {
            throw new NotFoundException("Intervention", id);
        }
        return i;
    }

    public InterventionAssignment requireMyAssignment(Intervention i) {
        InterventionAssignment a = findMyAssignment(i);
        if (a == null) {
            throw new BusinessException("Vous n'êtes pas affecté à cette intervention.");
        }
        return a;
    }

    private InterventionAssignment findMyAssignment(Intervention i) {
        Long me = currentUser.get().getEmployeeId();
        if (me == null) return null;
        return i.getAssignments().stream().filter(a -> a.getTechnician().getId().equals(me)).findFirst().orElse(null);
    }

    private void requireAssignedOrManager(Intervention i) {
        if (!currentUser.hasRole(Role.RESPONSABLE_TECHNIQUE)) {
            requireMyAssignment(i);
        }
    }

    private void requireEditable(Intervention i) {
        if (!InterventionStatus.EDITABLE.contains(i.getStatus())) {
            throw new BusinessException("Une intervention " + i.getStatus() + " ne peut plus être replanifiée.");
        }
    }

    private void requireScheduled(Intervention i) {
        if (!i.isScheduled()) {
            throw new BusinessException("Définissez d'abord le créneau de l'intervention.");
        }
    }

    private void changeStatus(Intervention i, InterventionStatus target, String comment) {
        InterventionStatus from = i.getStatus();
        if (!from.canTransitionTo(target)) {
            throw new BusinessException("Transition impossible : " + from + " -> " + target + ".");
        }
        i.setStatus(target);
        recordHistory(i, from, target, comment);
        events.publishEvent(new InterventionStatusChangedEvent(i, from, target));
    }

    private void recordHistory(Intervention i, InterventionStatus from, InterventionStatus to, String comment) {
        InterventionStatusHistory h = new InterventionStatusHistory();
        h.setIntervention(i);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(currentUser.displayName());
        h.setComment(comment);
        historyRepository.save(h);
    }

    private void applyDescriptiveFields(Intervention i, InterventionRequest req) {
        i.setTitle(req.title().trim());
        i.setDescription(req.description());
        i.setType(req.type());
        i.setPriority(req.priority());
        i.setLocation(req.location().trim());
        i.setRequiredSpecialty(req.requiredSpecialty());
        i.setDueDate(req.dueDate());
        i.setTeam(req.teamId() != null ? teamService.load(req.teamId()) : null);
    }

    /** @return true si le créneau a changé */
    private boolean applySchedule(Intervention i, LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            if (!i.getAssignments().isEmpty()) {
                throw new BusinessException("Impossible de retirer le créneau d'une intervention déjà affectée.");
            }
            boolean changed = i.isScheduled();
            i.setPlannedStart(null);
            i.setPlannedEnd(null);
            return changed;
        }
        DateRanges.requireOrdered(start, end);
        if (i.getId() == null && start.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BusinessException("Une nouvelle intervention ne peut pas être planifiée dans le passé.");
        }
        if (i.getDueDate() != null && end.toLocalDate().isAfter(i.getDueDate())) {
            throw new BusinessException("Le créneau dépasse l'échéance du " + i.getDueDate() + ".");
        }
        boolean changed = !start.equals(i.getPlannedStart()) || !end.equals(i.getPlannedEnd());
        i.setPlannedStart(start);
        i.setPlannedEnd(end);
        return changed;
    }

    private void applyResources(Intervention i, Long equipmentId, Long vehicleId) {
        if ((equipmentId != null || vehicleId != null) && !i.isScheduled()) {
            throw new BusinessException("Planifiez l'intervention avant de réserver un équipement ou un véhicule.");
        }
        List<String> problems = new ArrayList<>();
        Equipment eq = equipmentId != null ? equipmentService.load(equipmentId) : null;
        Vehicle v = vehicleId != null ? vehicleService.load(vehicleId) : null;
        if (eq != null) {
            availability.equipmentConflicts(eq, i.getPlannedStart(), i.getPlannedEnd(), i.getId())
                    .forEach(c -> problems.add(c.message()));
        }
        if (v != null) {
            availability.vehicleConflicts(v, i.getPlannedStart(), i.getPlannedEnd(), i.getId())
                    .forEach(c -> problems.add(c.message()));
        }
        if (!problems.isEmpty()) {
            throw new ConflictException("Ressource indisponible sur ce créneau.", problems);
        }
        i.setEquipment(eq);
        i.setVehicle(v);
    }

    /** Après un changement de créneau : statut, contrôle des techniciens déjà affectés, notifications. */
    private void afterScheduleChange(Intervention i, boolean datesChanged) {
        if (i.isScheduled() && i.getStatus() == InterventionStatus.NOUVELLE) {
            changeStatus(i, InterventionStatus.PLANIFIEE, null);
        }
        if (!datesChanged || i.getAssignments().isEmpty()) return;

        List<String> problems = new ArrayList<>();
        for (InterventionAssignment a : i.getAssignments()) {
            List<Conflict> c = availability.technicianConflicts(a.getTechnician(), i.getPlannedStart(),
                    i.getPlannedEnd(), i.getId());
            c.forEach(x -> problems.add(a.getTechnician().getFullName() + " : " + x.message()));
        }
        if (!problems.isEmpty()) {
            throw new ConflictException("Le nouveau créneau crée des conflits pour les techniciens affectés.", problems);
        }
        i.getAssignments().forEach(a -> notifications.notifyEmployee(a.getTechnician().getId(),
                NotificationType.INTERVENTION_MODIFIEE, "Changement de planning " + i.getReference(),
                "Nouveau créneau : " + i.getPlannedStart().toLocalDate() + " " + i.getPlannedStart().toLocalTime()
                        + "-" + i.getPlannedEnd().toLocalTime(), "/interventions/" + i.getId()));
    }

    private void setAssetsStatus(Intervention i, AssetStatus status) {
        if (i.getEquipment() != null && i.getEquipment().getStatus().isBookable()) i.getEquipment().setStatus(status);
        if (i.getVehicle() != null && i.getVehicle().getStatus().isBookable()) i.getVehicle().setStatus(status);
    }

    private InterventionResponse toResponse(Intervention i) {
        var team = i.getTeam();
        var eq = i.getEquipment();
        var v = i.getVehicle();
        List<AssignedTechnician> techs = i.getAssignments().stream()
                .map(a -> new AssignedTechnician(a.getTechnician().getId(), a.getTechnician().getFullName(),
                        a.getTechnician().getSpecialty(), a.getAssignedAt(), a.getAcknowledgedAt()))
                .toList();
        return new InterventionResponse(i.getId(), i.getReference(), i.getTitle(), i.getDescription(), i.getType(),
                i.getPriority(), i.getLocation(), i.getRequiredSpecialty(), i.getPlannedStart(), i.getPlannedEnd(),
                i.getDueDate(), i.getStatus(), i.isLate(), team != null ? team.getId() : null,
                team != null ? team.getName() : null, eq != null ? eq.getId() : null, eq != null ? eq.getName() : null,
                v != null ? v.getId() : null, v != null ? v.getPlateNumber() : null,
                i.getCreatedBy() != null ? i.getCreatedBy().getEmail() : null, i.getCreatedAt(),
                i.getActualStart(), i.getActualEnd(), i.getCancelReason(), techs);
    }
}
