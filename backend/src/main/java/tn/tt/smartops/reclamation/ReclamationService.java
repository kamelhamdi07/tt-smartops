package tn.tt.smartops.reclamation;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.auth.Role;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.employee.EmployeeRepository;
import tn.tt.smartops.employee.EmployeeStatus;
import tn.tt.smartops.intervention.Intervention;
import tn.tt.smartops.intervention.InterventionDtos.InterventionRequest;
import tn.tt.smartops.intervention.InterventionService;
import tn.tt.smartops.intervention.InterventionStatusChangedEvent;
import tn.tt.smartops.intervention.InterventionType;
import tn.tt.smartops.notification.NotificationService;
import tn.tt.smartops.notification.NotificationType;
import tn.tt.smartops.reclamation.ReclamationDtos.*;
import tn.tt.smartops.security.CurrentUser;
import tn.tt.smartops.team.Team;
import tn.tt.smartops.team.TeamRepository;
import tn.tt.smartops.team.TeamService;
import tn.tt.smartops.vehicle.VehicleRepository;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cycle d'une réclamation client :
 * le client la dépose -> elle est routée vers l'équipe de sa catégorie -> le chef d'équipe
 * choisit technicien, véhicule et créneau (une intervention est créée) -> le statut suit
 * ensuite l'intervention jusqu'à RESOLUE lorsque le technicien la termine.
 */
@Service
@RequiredArgsConstructor
public class ReclamationService {

    private static final DateTimeFormatter SLOT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final ReclamationRepository repository;
    private final TeamRepository teamRepository;
    private final TeamService teamService;
    private final EmployeeRepository employeeRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final InterventionService interventionService;
    private final CurrentUser currentUser;
    private final NotificationService notifications;
    private final AuditService audit;

    // ---------------------------------------------------------------- client

    @Transactional
    public ReclamationResponse create(ReclamationRequest req) {
        User client = userRepository.findById(currentUser.get().getId()).orElseThrow();
        Reclamation r = new Reclamation();
        r.setReference(String.format("REC-%d-%05d", Year.now().getValue(), repository.nextReferenceNumber()));
        r.setClient(client);
        r.setCategory(req.category());
        r.setTitle(req.title().trim());
        r.setDescription(req.description().trim());
        r.setAddress(firstNonBlank(req.address(), client.getAddress(), "Adresse non précisée"));
        r.setPhone(firstNonBlank(req.phone(), client.getPhone(), "—"));
        r.setTeam(teamRepository.findByCategory(req.category()).orElse(null));
        repository.save(r);
        audit.log("RECLAMATION_CREATED", "Reclamation", r.getId(), r.getReference());
        notifyTeam(r, "Nouvelle réclamation " + r.getReference());
        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReclamationResponse> mine(int page, int size) {
        Long me = currentUser.get().getId();
        return PageResponse.of(repository.findByClientIdOrderByCreatedAtDesc(me, PageRequest.of(page, Math.min(size, 50)))
                .map(this::toResponse));
    }

    @Transactional
    public ReclamationResponse cancel(Long id) {
        Reclamation r = load(id);
        if (!r.getClient().getId().equals(currentUser.get().getId())) {
            throw new NotFoundException("Réclamation", id);
        }
        if (r.getStatus() != ReclamationStatus.NOUVELLE) {
            throw new BusinessException("Une réclamation déjà prise en charge ne peut plus être annulée.");
        }
        r.setStatus(ReclamationStatus.ANNULEE);
        audit.log("RECLAMATION_CANCELLED", "Reclamation", id, r.getReference());
        return toResponse(r);
    }

    // ---------------------------------------------------------------- personnel

    /** Responsable technique et admin voient tout ; un chef d'équipe voit les réclamations de ses équipes. */
    @Transactional(readOnly = true)
    public PageResponse<ReclamationResponse> search(ReclamationStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Reclamation> result;
        if (currentUser.hasRole(Role.ADMIN, Role.RESPONSABLE_TECHNIQUE)) {
            result = repository.searchAll(status, pageable);
        } else {
            Set<Long> teams = ledTeamIds();
            result = teams.isEmpty() ? Page.empty(pageable) : repository.searchTeams(teams, status, pageable);
        }
        return PageResponse.of(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ReclamationResponse get(Long id) {
        Reclamation r = load(id);
        boolean owner = r.getClient().getId().equals(currentUser.get().getId());
        if (!owner && !canView(r)) {
            throw new NotFoundException("Réclamation", id);
        }
        return toResponse(r);
    }

    /** Techniciens de l'équipe et véhicules utilisables, pour le formulaire du chef d'équipe. */
    @Transactional(readOnly = true)
    public DispatchOptions options(Long id) {
        Reclamation r = load(id);
        requireCanDispatch(r);
        Long teamId = r.getTeam().getId();
        List<TechnicianOption> techs = employeeRepository.findByTechnicianTrueAndStatusOrderByLastNameAsc(EmployeeStatus.ACTIF)
                .stream()
                .filter(e -> e.getTeam() != null && e.getTeam().getId().equals(teamId))
                .map(e -> new TechnicianOption(e.getId(), e.getFullName(), e.getSpecialty()))
                .toList();
        List<VehicleOption> vehicles = vehicleRepository.findByStatusInOrderByPlateNumberAsc(
                        List.of(AssetStatus.DISPONIBLE, AssetStatus.AFFECTE)).stream()
                // les véhicules de l'équipe d'abord
                .sorted(Comparator.comparing(v -> v.getTeam() == null || !v.getTeam().getId().equals(teamId)))
                .map(v -> new VehicleOption(v.getId(), v.getPlateNumber(), v.getBrandModel(),
                        v.getTeam() != null ? v.getTeam().getName() : null))
                .toList();
        return new DispatchOptions(techs, vehicles);
    }

    /** Le chef d'équipe affecte un technicien et un véhicule : une intervention de dépannage est créée. */
    @Transactional
    public ReclamationResponse dispatch(Long id, DispatchRequest req) {
        Reclamation r = load(id);
        requireCanDispatch(r);
        if (r.getStatus() != ReclamationStatus.NOUVELLE) {
            throw new BusinessException("Cette réclamation est déjà prise en charge.");
        }
        Employee tech = employeeRepository.findById(req.technicianId())
                .orElseThrow(() -> new NotFoundException("Employé", req.technicianId()));
        if (!currentUser.hasRole(Role.RESPONSABLE_TECHNIQUE)
                && (tech.getTeam() == null || !tech.getTeam().getId().equals(r.getTeam().getId()))) {
            throw new BusinessException(tech.getFullName() + " ne fait pas partie de l'équipe " + r.getTeam().getName() + ".");
        }

        User client = r.getClient();
        String description = "Réclamation client " + r.getReference() + " (" + r.getCategory() + ")\n"
                + "Client : " + displayName(client) + " · Tél. " + r.getPhone() + "\n"
                + "Adresse : " + r.getAddress() + "\n\n" + r.getDescription();
        var created = interventionService.create(new InterventionRequest(
                r.getReference() + " · " + r.getTitle(), description, InterventionType.DEPANNAGE, req.priority(),
                r.getAddress(), null, req.plannedStart(), req.plannedEnd(), null, r.getTeam().getId(), null,
                req.vehicleId()));
        r.setIntervention(interventionService.load(created.id()));
        r.setStatus(ReclamationStatus.AFFECTEE);
        interventionService.assign(created.id(), tech.getId());

        audit.log("RECLAMATION_DISPATCHED", "Reclamation", id, created.reference() + " -> " + tech.getMatricule());
        notifyClient(r, "Votre réclamation " + r.getReference() + " est prise en charge",
                "Technicien : " + tech.getFullName() + " · passage prévu le " + req.plannedStart().format(SLOT) + ".");
        return toResponse(r);
    }

    /** Routage manuel d'une réclamation dont la catégorie n'est couverte par aucune équipe. */
    @Transactional
    public ReclamationResponse route(Long id, RouteRequest req) {
        Reclamation r = load(id);
        if (r.getStatus() != ReclamationStatus.NOUVELLE) {
            throw new BusinessException("Seule une réclamation non traitée peut changer d'équipe.");
        }
        r.setTeam(teamService.load(req.teamId()));
        audit.log("RECLAMATION_ROUTED", "Reclamation", id, "équipe " + req.teamId());
        notifyTeam(r, "Réclamation " + r.getReference() + " transmise à votre équipe");
        return toResponse(r);
    }

    // ---------------------------------------------------------------- suivi automatique

    /** Le statut de la réclamation suit celui de l'intervention qui la traite. */
    @EventListener
    public void onInterventionStatusChanged(InterventionStatusChangedEvent event) {
        repository.findByInterventionId(event.intervention().getId()).ifPresent(r -> {
            switch (event.to()) {
                case EN_COURS -> {
                    if (r.getStatus() == ReclamationStatus.AFFECTEE) {
                        r.setStatus(ReclamationStatus.EN_COURS);
                        notifyClient(r, "Intervention en cours · " + r.getReference(),
                                "Le technicien a commencé le traitement de votre réclamation.");
                    }
                }
                case TERMINEE -> {
                    r.setStatus(ReclamationStatus.RESOLUE);
                    r.setResolvedAt(LocalDateTime.now());
                    notifyClient(r, "Réclamation résolue · " + r.getReference(),
                            "Le technicien a terminé l'intervention. Votre problème est réglé.");
                }
                case ANNULEE -> {
                    // retour au chef d'équipe pour une nouvelle affectation
                    r.setStatus(ReclamationStatus.NOUVELLE);
                    r.setIntervention(null);
                    notifyTeam(r, "Réclamation " + r.getReference() + " à réaffecter");
                }
                default -> { }
            }
        });
    }

    // ---------------------------------------------------------------- règles internes

    private Reclamation load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Réclamation", id));
    }

    private Set<Long> ledTeamIds() {
        Long employeeId = currentUser.get().getEmployeeId();
        if (employeeId == null) return Set.of();
        return teamRepository.findByLeaderId(employeeId).stream().map(Team::getId).collect(Collectors.toSet());
    }

    private boolean canView(Reclamation r) {
        return currentUser.hasRole(Role.ADMIN, Role.RESPONSABLE_TECHNIQUE)
                || (r.getTeam() != null && ledTeamIds().contains(r.getTeam().getId()));
    }

    private boolean canDispatch(Reclamation r) {
        if (r.getTeam() == null) return false;
        return currentUser.hasRole(Role.RESPONSABLE_TECHNIQUE) || ledTeamIds().contains(r.getTeam().getId());
    }

    private void requireCanDispatch(Reclamation r) {
        if (r.getTeam() == null) {
            throw new BusinessException("Aucune équipe ne prend en charge cette réclamation : transmettez-la d'abord à une équipe.");
        }
        if (!canDispatch(r)) {
            throw new BusinessException("Seul le chef de l'équipe " + r.getTeam().getName() + " peut traiter cette réclamation.");
        }
    }

    private void notifyTeam(Reclamation r, String title) {
        String message = r.getTitle() + " · " + r.getAddress();
        Team team = r.getTeam();
        if (team != null && team.getLeader() != null) {
            notifications.notifyEmployee(team.getLeader().getId(), NotificationType.RECLAMATION_NOUVELLE, title, message, "/reclamations");
        } else {
            notifications.notifyRole(Role.RESPONSABLE_TECHNIQUE, NotificationType.RECLAMATION_NOUVELLE, title,
                    message + (team == null ? " · aucune équipe pour la catégorie " + r.getCategory() : ""), "/reclamations");
        }
    }

    private void notifyClient(Reclamation r, String title, String message) {
        notifications.notifyUser(r.getClient(), NotificationType.RECLAMATION_MISE_A_JOUR, title, message, "/my-reclamations");
    }

    private static String displayName(User u) {
        return u.getFullName() != null ? u.getFullName() : u.getEmail();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    private ReclamationResponse toResponse(Reclamation r) {
        Team team = r.getTeam();
        Intervention i = r.getIntervention();
        List<String> techs = i == null ? List.of()
                : i.getAssignments().stream().map(a -> a.getTechnician().getFullName()).filter(Objects::nonNull).toList();
        boolean dispatchable = r.getStatus() == ReclamationStatus.NOUVELLE
                && !currentUser.hasRole(Role.CLIENT) && canDispatch(r);
        return new ReclamationResponse(r.getId(), r.getReference(), r.getCategory(), r.getTitle(), r.getDescription(),
                r.getAddress(), r.getPhone(), r.getStatus(), displayName(r.getClient()), r.getClient().getEmail(),
                team != null ? team.getId() : null, team != null ? team.getName() : null,
                i != null ? i.getId() : null, i != null ? i.getReference() : null, techs,
                i != null && i.getVehicle() != null ? i.getVehicle().getPlateNumber() : null,
                i != null ? i.getPlannedStart() : null, i != null ? i.getPlannedEnd() : null,
                r.getCreatedAt(), r.getResolvedAt(), dispatchable);
    }
}
