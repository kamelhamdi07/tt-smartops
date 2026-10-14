package tn.tt.smartops.dashboard;

import tn.tt.smartops.reclamation.ReclamationRepository;
import tn.tt.smartops.reclamation.ReclamationStatus;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.absence.AbsenceRepository;
import tn.tt.smartops.audit.AuditLogRepository;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.dashboard.DashboardDtos.*;
import tn.tt.smartops.department.DepartmentRepository;
import tn.tt.smartops.employee.EmployeeRepository;
import tn.tt.smartops.employee.EmployeeStatus;
import tn.tt.smartops.intervention.InterventionAssignmentRepository;
import tn.tt.smartops.intervention.InterventionRepository;
import tn.tt.smartops.intervention.InterventionStatus;
import tn.tt.smartops.leave.LeaveRequestRepository;
import tn.tt.smartops.leave.LeaveStatus;
import tn.tt.smartops.notification.NotificationRepository;
import tn.tt.smartops.planning.PlanningService;
import tn.tt.smartops.report.InterventionReportRepository;
import tn.tt.smartops.report.ReportStatus;
import tn.tt.smartops.security.AppUserDetails;
import tn.tt.smartops.security.CurrentUser;
import tn.tt.smartops.team.TeamRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Indicateurs adaptés au rôle de l'utilisateur connecté. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final LeaveRequestRepository leaveRepository;
    private final AbsenceRepository absenceRepository;
    private final InterventionRepository interventionRepository;
    private final InterventionAssignmentRepository assignmentRepository;
    private final InterventionReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditRepository;
    private final PlanningService planningService;
    private final ReclamationRepository reclamationRepository;

    @Transactional(readOnly = true)
    public DashboardResponse forCurrentUser() {
        AppUserDetails me = currentUser.get();
        return switch (me.getRole()) {
            case ADMIN -> admin(me);
            case RH -> rh(me);
            case RESPONSABLE_TECHNIQUE -> technicalManager(me);
            case TECHNICIEN -> technician(me);
            case EMPLOYE -> employee(me);
            case CLIENT -> client(me);
        };
    }

    private DashboardResponse admin(AppUserDetails me) {
        List<Kpi> kpis = List.of(
                new Kpi("users", "Comptes actifs", userRepository.countByEnabledTrue(), "info", null),
                new Kpi("employees", "Employés actifs", employeeRepository.countByStatus(EmployeeStatus.ACTIF), "neutral", null),
                new Kpi("open", "Interventions ouvertes", openInterventions(), "neutral", null),
                new Kpi("audit24h", "Opérations tracées (24 h)",
                        auditRepository.countByCreatedAtAfter(LocalDateTime.now().minusHours(24)), "neutral", "/audit"));
        return new DashboardResponse(me.getRole(), kpis, List.of(byStatus(), byPriority()));
    }

    private DashboardResponse rh(AppUserDetails me) {
        LocalDate today = LocalDate.now();
        List<Kpi> kpis = List.of(
                new Kpi("employees", "Employés actifs", employeeRepository.countByStatus(EmployeeStatus.ACTIF), "neutral", "/employees"),
                new Kpi("technicians", "Techniciens actifs", employeeRepository.countByTechnicianTrueAndStatus(EmployeeStatus.ACTIF), "neutral", "/employees"),
                new Kpi("departments", "Départements", departmentRepository.count(), "neutral", null),
                new Kpi("teams", "Équipes", teamRepository.count(), "neutral", null),
                new Kpi("pendingLeaves", "Congés à traiter", leaveRepository.countByStatus(LeaveStatus.EN_ATTENTE), "warning", "/leaves/manage"),
                new Kpi("onLeave", "En congé aujourd'hui", leaveRepository.countEmployeesOnLeave(today), "info", null),
                new Kpi("absent", "Absents aujourd'hui", absenceRepository.countEmployeesAbsent(today), "danger", null));
        return new DashboardResponse(me.getRole(), kpis,
                List.of(new Chart("leavesByType", "Demandes de congé par type", points(leaveRepository.countByType()))));
    }

    private DashboardResponse technicalManager(AppUserDetails me) {
        LocalDateTime now = LocalDateTime.now();
        long conflicts = planningService.detectConflicts(now, now.plusDays(7)).size();
        List<Kpi> kpis = new ArrayList<>(List.of(
                new Kpi("new", "Nouvelles", interventionRepository.countByStatus(InterventionStatus.NOUVELLE), "info", "/interventions?status=NOUVELLE"),
                new Kpi("planned", "Planifiées / affectées",
                        interventionRepository.countByStatus(InterventionStatus.PLANIFIEE)
                                + interventionRepository.countByStatus(InterventionStatus.AFFECTEE), "neutral", "/planning"),
                new Kpi("inProgress", "En cours", interventionRepository.countByStatus(InterventionStatus.EN_COURS), "info", "/interventions?status=EN_COURS"),
                new Kpi("done", "Terminées, à clôturer", interventionRepository.countByStatus(InterventionStatus.TERMINEE), "success", "/interventions?status=TERMINEE"),
                new Kpi("late", "En retard", interventionRepository.countLate(InterventionStatus.BLOCKING, now, now.toLocalDate()), "danger", null),
                new Kpi("reports", "Rapports à valider", reportRepository.countByStatus(ReportStatus.SOUMIS), "warning", null),
                new Kpi("conflicts", "Conflits sur 7 jours", conflicts, conflicts > 0 ? "danger" : "success", "/planning")));
        return new DashboardResponse(me.getRole(), kpis, List.of(byStatus(), byPriority(),
                new Chart("byTeam", "Interventions par équipe", points(interventionRepository.countGroupByTeam())),
                new Chart("byTechnician", "Charge par technicien",
                        points(assignmentRepository.countGroupByTechnician()).stream().limit(10).toList()),
                new Chart("avgDuration", "Durée moyenne réalisée (min)", List.of(
                        new ChartPoint("Durée moyenne", Math.round(interventionRepository.averageDurationMinutes()))))));
    }

    private DashboardResponse technician(AppUserDetails me) {
        Long empId = currentUser.requireEmployeeId();
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        long today = planningService.myEntries(dayStart, dayStart.plusDays(1)).size();
        List<Kpi> kpis = List.of(
                new Kpi("today", "Interventions du jour", today, "info", "/planning"),
                new Kpi("upcoming", "À venir", assignmentRepository.countForTechnician(empId,
                        EnumSet.of(InterventionStatus.PLANIFIEE, InterventionStatus.AFFECTEE)), "neutral", "/interventions"),
                new Kpi("inProgress", "En cours", assignmentRepository.countForTechnician(empId,
                        EnumSet.of(InterventionStatus.EN_COURS, InterventionStatus.SUSPENDUE)), "warning", "/interventions"),
                new Kpi("reports", "Rapports à compléter", reportRepository.countForAuthor(empId,
                        EnumSet.of(ReportStatus.BROUILLON, ReportStatus.A_CORRIGER)), "danger", "/interventions"));
        return new DashboardResponse(me.getRole(), kpis, List.of());
    }

    private DashboardResponse client(AppUserDetails me) {
        List<Kpi> kpis = List.of(
                new Kpi("open", "Réclamations en cours",
                        reclamationRepository.countByClientIdAndStatusIn(me.getId(), ReclamationStatus.OPEN), "warning", "/my-reclamations"),
                new Kpi("resolved", "Réclamations résolues",
                        reclamationRepository.countByClientIdAndStatusIn(me.getId(), Set.of(ReclamationStatus.RESOLUE)), "success", "/my-reclamations"),
                new Kpi("unread", "Notifications non lues",
                        notificationRepository.countByRecipientIdAndReadFalse(me.getId()), "info", "/notifications"));
        return new DashboardResponse(me.getRole(), kpis, List.of());
    }

    private DashboardResponse employee(AppUserDetails me) {
        Long empId = currentUser.requireEmployeeId();
        var leaves = leaveRepository.findByEmployeeId(empId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<Kpi> kpis = List.of(
                new Kpi("pending", "Demandes en attente",
                        leaves.stream().filter(l -> l.getStatus() == LeaveStatus.EN_ATTENTE).count(), "warning", "/leaves"),
                new Kpi("approvedDays", "Jours de congé approuvés (année)",
                        leaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROUVEE
                                && l.getStartDate().getYear() == LocalDate.now().getYear())
                                .mapToLong(l -> l.getWorkingDays()).sum(), "success", "/leaves"),
                new Kpi("unread", "Notifications non lues",
                        notificationRepository.countByRecipientIdAndReadFalse(me.getId()), "info", "/notifications"));
        return new DashboardResponse(me.getRole(), kpis, List.of());
    }

    private long openInterventions() {
        return InterventionStatus.BLOCKING.stream().mapToLong(interventionRepository::countByStatus).sum()
                + interventionRepository.countByStatus(InterventionStatus.NOUVELLE);
    }

    private Chart byStatus() {
        return new Chart("byStatus", "Interventions par statut", points(interventionRepository.countGroupByStatus()));
    }

    private Chart byPriority() {
        return new Chart("byPriority", "Interventions par priorité", points(interventionRepository.countGroupByPriority()));
    }

    private static List<ChartPoint> points(List<Object[]> rows) {
        return rows.stream().map(r -> new ChartPoint(String.valueOf(r[0]), ((Number) r[1]).longValue())).toList();
    }
}
