package tn.tt.smartops.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.employee.EmployeeService;
import tn.tt.smartops.intervention.Intervention;
import tn.tt.smartops.intervention.InterventionService;
import tn.tt.smartops.intervention.InterventionStatus;
import tn.tt.smartops.notification.NotificationService;
import tn.tt.smartops.notification.NotificationType;
import tn.tt.smartops.report.ReportDtos.*;
import tn.tt.smartops.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final EnumSet<InterventionStatus> WRITABLE_WHEN =
            EnumSet.of(InterventionStatus.EN_COURS, InterventionStatus.SUSPENDUE, InterventionStatus.TERMINEE);

    private final InterventionReportRepository repository;
    private final InterventionService interventionService;
    private final EmployeeService employeeService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final NotificationService notifications;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public ReportResponse get(Long interventionId) {
        interventionService.loadVisible(interventionId);
        return repository.findByInterventionId(interventionId).map(ReportResponse::from)
                .orElseThrow(() -> new NotFoundException("Rapport de l'intervention", interventionId));
    }

    /** Le technicien affecté rédige ou corrige le rapport. */
    @Transactional
    public ReportResponse save(Long interventionId, ReportRequest req) {
        Intervention i = interventionService.load(interventionId);
        interventionService.requireMyAssignment(i);
        if (!WRITABLE_WHEN.contains(i.getStatus())) {
            throw new BusinessException("Le rapport ne peut être rédigé qu'une fois l'intervention démarrée.");
        }
        if (req.workStart() != null && req.workEnd() != null && !req.workEnd().isAfter(req.workStart())) {
            throw new BusinessException("L'heure de fin des travaux doit suivre l'heure de début.");
        }
        InterventionReport r = repository.findByInterventionId(interventionId).orElseGet(() -> {
            InterventionReport created = new InterventionReport();
            created.setIntervention(i);
            created.setAuthor(employeeService.load(currentUser.requireEmployeeId()));
            return created;
        });
        if (r.getStatus() == ReportStatus.SOUMIS || r.getStatus() == ReportStatus.VALIDE) {
            throw new BusinessException("Le rapport est " + r.getStatus() + " et n'est plus modifiable.");
        }
        r.setWorkStart(req.workStart());
        r.setWorkEnd(req.workEnd());
        r.setProblemFound(req.problemFound());
        r.setDiagnosis(req.diagnosis());
        r.setSolutionApplied(req.solutionApplied());
        r.setMaterialsUsed(req.materialsUsed());
        r.setReplacedParts(req.replacedParts());
        r.setObservations(req.observations());
        r.setResult(req.result());
        repository.save(r);
        return ReportResponse.from(r);
    }

    @Transactional
    public ReportResponse submit(Long interventionId) {
        Intervention i = interventionService.load(interventionId);
        interventionService.requireMyAssignment(i);
        if (i.getStatus() != InterventionStatus.TERMINEE) {
            throw new BusinessException("Terminez l'intervention avant de soumettre le rapport.");
        }
        InterventionReport r = repository.findByInterventionId(interventionId)
                .orElseThrow(() -> new BusinessException("Aucun rapport à soumettre."));
        if (r.getStatus() != ReportStatus.BROUILLON && r.getStatus() != ReportStatus.A_CORRIGER) {
            throw new BusinessException("Ce rapport a déjà été soumis.");
        }
        if (isBlank(r.getDiagnosis()) || isBlank(r.getSolutionApplied()) || r.getResult() == null
                || r.getWorkStart() == null || r.getWorkEnd() == null) {
            throw new BusinessException("Diagnostic, solution, résultat et horaires sont obligatoires pour soumettre.");
        }
        r.setStatus(ReportStatus.SOUMIS);
        r.setSubmittedAt(LocalDateTime.now());
        audit.log("REPORT_SUBMITTED", "InterventionReport", r.getId(), i.getReference());
        notifications.notifyUser(i.getCreatedBy(), NotificationType.RAPPORT_A_VALIDER,
                "Rapport à valider " + i.getReference(),
                "Le rapport de l'intervention " + i.getReference() + " est prêt à être validé.",
                "/interventions/" + i.getId());
        return ReportResponse.from(r);
    }

    @Transactional
    public ReportResponse review(Long interventionId, ReviewRequest req) {
        Intervention i = interventionService.load(interventionId);
        InterventionReport r = repository.findByInterventionId(interventionId)
                .orElseThrow(() -> new NotFoundException("Rapport de l'intervention", interventionId));
        if (r.getStatus() != ReportStatus.SOUMIS) {
            throw new BusinessException("Seul un rapport soumis peut être examiné.");
        }
        if (!req.approve() && isBlank(req.comment())) {
            throw new BusinessException("Précisez la correction attendue.");
        }
        r.setStatus(req.approve() ? ReportStatus.VALIDE : ReportStatus.A_CORRIGER);
        r.setReviewComment(req.comment());
        r.setReviewedBy(userRepository.getReferenceById(currentUser.get().getId()));
        r.setReviewedAt(LocalDateTime.now());

        if (req.approve()) {
            interventionService.close(i, "Rapport validé");
        }
        audit.log(req.approve() ? "REPORT_VALIDATED" : "REPORT_CORRECTION_REQUESTED",
                "InterventionReport", r.getId(), req.comment());
        notifications.notifyEmployee(r.getAuthor().getId(),
                req.approve() ? NotificationType.RAPPORT_VALIDE : NotificationType.RAPPORT_A_CORRIGER,
                (req.approve() ? "Rapport validé " : "Rapport à corriger ") + i.getReference(),
                req.approve() ? "Votre rapport a été validé, l'intervention est clôturée."
                        : "Correction demandée : " + req.comment(),
                "/interventions/" + i.getId());
        return ReportResponse.from(r);
    }

    @Transactional(readOnly = true)
    public List<PendingReport> pendingReview() {
        return repository.findByStatusOrderBySubmittedAtAsc(ReportStatus.SOUMIS).stream()
                .map(r -> new PendingReport(r.getId(), r.getIntervention().getId(), r.getIntervention().getReference(),
                        r.getIntervention().getTitle(), r.getAuthor().getFullName(), r.getSubmittedAt()))
                .toList();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
