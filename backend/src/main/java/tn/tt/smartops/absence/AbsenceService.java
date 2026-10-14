package tn.tt.smartops.absence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.absence.AbsenceDtos.AbsenceRequest;
import tn.tt.smartops.absence.AbsenceDtos.AbsenceResponse;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.common.DateRanges;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.employee.EmployeeService;
import tn.tt.smartops.security.CurrentUser;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AbsenceService {

    private final AbsenceRepository repository;
    private final EmployeeService employeeService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final AuditService audit;

    @Transactional
    public AbsenceResponse create(AbsenceRequest req) {
        Absence a = new Absence();
        apply(a, req, null);
        a.setRecordedBy(userRepository.getReferenceById(currentUser.get().getId()));
        repository.save(a);
        audit.log("ABSENCE_RECORDED", "Absence", a.getId(),
                a.getEmployee().getMatricule() + " " + req.startDate() + " -> " + req.endDate());
        return AbsenceResponse.from(a);
    }

    @Transactional
    public AbsenceResponse update(Long id, AbsenceRequest req) {
        Absence a = load(id);
        apply(a, req, id);
        audit.log("ABSENCE_UPDATED", "Absence", id, req.startDate() + " -> " + req.endDate());
        return AbsenceResponse.from(a);
    }

    @Transactional
    public void delete(Long id) {
        Absence a = load(id);
        repository.delete(a);
        audit.log("ABSENCE_DELETED", "Absence", id, a.getEmployee().getMatricule());
    }

    @Transactional(readOnly = true)
    public PageResponse<AbsenceResponse> search(Long employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        return PageResponse.of(repository.search(employeeId, from, to, pageable).map(AbsenceResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<AbsenceResponse> mine(Pageable pageable) {
        return search(currentUser.requireEmployeeId(), null, null, pageable);
    }

    private void apply(Absence a, AbsenceRequest req, Long excludeId) {
        DateRanges.requireOrdered(req.startDate(), req.endDate());
        if (!repository.findOverlapping(req.employeeId(), req.startDate(), req.endDate(), excludeId).isEmpty()) {
            throw new ConflictException("Une absence est déjà enregistrée sur cette période pour cet employé.");
        }
        a.setEmployee(employeeService.load(req.employeeId()));
        a.setType(req.type());
        a.setStartDate(req.startDate());
        a.setEndDate(req.endDate());
        a.setReason(req.reason());
        a.setJustified(req.justified());
    }

    private Absence load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Absence", id));
    }
}
