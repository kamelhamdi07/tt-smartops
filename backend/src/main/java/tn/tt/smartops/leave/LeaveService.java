package tn.tt.smartops.leave;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.auth.Role;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.common.DateRanges;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.employee.EmployeeService;
import tn.tt.smartops.leave.LeaveDtos.*;
import tn.tt.smartops.notification.NotificationService;
import tn.tt.smartops.notification.NotificationType;
import tn.tt.smartops.security.CurrentUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Workflow : EN_ATTENTE -> APPROUVEE | REFUSEE (RH), ou EN_ATTENTE -> ANNULEE (demandeur).
 */
@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final List<LeaveStatus> ACTIVE = List.of(LeaveStatus.EN_ATTENTE, LeaveStatus.APPROUVEE);

    private final LeaveRequestRepository repository;
    private final EmployeeService employeeService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final NotificationService notifications;
    private final AuditService audit;

    @Transactional
    public LeaveResponse create(LeaveCreateRequest req) {
        Employee employee = employeeService.load(currentUser.requireEmployeeId());
        DateRanges.requireOrdered(req.startDate(), req.endDate());
        if (req.type() != LeaveType.MALADIE && req.startDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Une demande de congé ne peut pas commencer dans le passé.");
        }
        int days = DateRanges.workingDays(req.startDate(), req.endDate());
        if (days == 0) {
            throw new BusinessException("La période choisie ne contient aucun jour ouvré.");
        }
        if (repository.existsOverlap(employee.getId(), req.startDate(), req.endDate(), ACTIVE, null)) {
            throw new ConflictException("Une demande en attente ou approuvée chevauche déjà cette période.");
        }
        LeaveRequest l = new LeaveRequest();
        l.setEmployee(employee);
        l.setType(req.type());
        l.setStartDate(req.startDate());
        l.setEndDate(req.endDate());
        l.setWorkingDays(days);
        l.setComment(req.comment());
        repository.save(l);

        audit.log("LEAVE_REQUESTED", "LeaveRequest", l.getId(), req.startDate() + " -> " + req.endDate());
        notifications.notifyRole(Role.RH, NotificationType.CONGE_DEMANDE, "Nouvelle demande de congé",
                employee.getFullName() + " demande " + days + " jour(s) du " + req.startDate() + " au " + req.endDate() + ".",
                "/leaves/manage");
        return LeaveResponse.from(l);
    }

    @Transactional
    public LeaveResponse cancel(Long id) {
        LeaveRequest l = load(id);
        if (!l.getEmployee().getId().equals(currentUser.requireEmployeeId())) {
            throw new NotFoundException("Demande de congé", id);
        }
        if (l.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new BusinessException("Seule une demande en attente peut être annulée.");
        }
        l.setStatus(LeaveStatus.ANNULEE);
        audit.log("LEAVE_CANCELLED", "LeaveRequest", id, null);
        return LeaveResponse.from(l);
    }

    @Transactional
    public LeaveResponse decide(Long id, LeaveDecisionRequest req) {
        LeaveRequest l = load(id);
        Long myEmployeeId = currentUser.get().getEmployeeId();
        if (myEmployeeId != null && myEmployeeId.equals(l.getEmployee().getId())) {
            throw new BusinessException("Vous ne pouvez pas traiter votre propre demande de congé.");
        }
        if (l.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new BusinessException("Cette demande a déjà été traitée (" + l.getStatus() + ").");
        }
        if (req.approve() && repository.existsOverlap(l.getEmployee().getId(), l.getStartDate(), l.getEndDate(),
                List.of(LeaveStatus.APPROUVEE), l.getId())) {
            throw new ConflictException("Un congé déjà approuvé chevauche cette période.");
        }
        l.setStatus(req.approve() ? LeaveStatus.APPROUVEE : LeaveStatus.REFUSEE);
        l.setDecisionComment(req.comment());
        l.setProcessedAt(LocalDateTime.now());
        l.setProcessedBy(userRepository.getReferenceById(currentUser.get().getId()));

        audit.log(req.approve() ? "LEAVE_APPROVED" : "LEAVE_REJECTED", "LeaveRequest", id, req.comment());
        notifications.notifyEmployee(l.getEmployee().getId(),
                req.approve() ? NotificationType.CONGE_APPROUVE : NotificationType.CONGE_REFUSE,
                req.approve() ? "Congé approuvé" : "Congé refusé",
                "Votre demande du " + l.getStartDate() + " au " + l.getEndDate() + " a été "
                        + (req.approve() ? "approuvée." : "refusée.")
                        + (req.comment() != null && !req.comment().isBlank() ? " Motif : " + req.comment() : ""),
                "/leaves");
        return LeaveResponse.from(l);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveResponse> mine(Pageable pageable) {
        return PageResponse.of(repository.findByEmployeeId(currentUser.requireEmployeeId(), pageable)
                .map(LeaveResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveResponse> search(LeaveStatus status, Long employeeId, Pageable pageable) {
        return PageResponse.of(repository.search(status, employeeId, pageable).map(LeaveResponse::from));
    }

    private LeaveRequest load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Demande de congé", id));
    }
}
