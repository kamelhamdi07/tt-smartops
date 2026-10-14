package tn.tt.smartops.employee;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.department.DepartmentService;
import tn.tt.smartops.employee.EmployeeDtos.EmployeeRequest;
import tn.tt.smartops.employee.EmployeeDtos.EmployeeResponse;
import tn.tt.smartops.employee.EmployeeDtos.EmployeeSummary;
import tn.tt.smartops.security.CurrentUser;
import tn.tt.smartops.team.Team;
import tn.tt.smartops.team.TeamService;

import java.util.List;


@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(String q, Long departmentId, Long teamId, EmployeeStatus status,
                                                 Boolean technician, Specialty specialty, Pageable pageable) {
        Specification<Employee> spec = Specification.allOf(
                EmployeeSpecifications.search(q),
                EmployeeSpecifications.department(departmentId),
                EmployeeSpecifications.team(teamId),
                EmployeeSpecifications.status(status),
                EmployeeSpecifications.technician(technician),
                EmployeeSpecifications.specialty(specialty));
        return PageResponse.of(repository.findAll(spec, pageable).map(EmployeeResponse::from));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) {
        return EmployeeResponse.from(load(id));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse me() {
        return get(currentUser.requireEmployeeId());
    }

    @Transactional(readOnly = true)
    public List<EmployeeSummary> activeTechnicians() {
        return repository.findByTechnicianTrueAndStatusOrderByLastNameAsc(EmployeeStatus.ACTIF)
                .stream().map(EmployeeSummary::from).toList();
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest req) {
        if (repository.existsByMatriculeIgnoreCase(req.matricule())) {
            throw new ConflictException("Le matricule " + req.matricule() + " est déjà utilisé.");
        }
        if (repository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("L'email " + req.email() + " est déjà utilisé.");
        }
        Employee e = new Employee();
        apply(e, req);
        repository.save(e);
        audit.log("EMPLOYEE_CREATED", "Employee", e.getId(), e.getMatricule());
        return EmployeeResponse.from(e);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest req) {
        Employee e = load(id);
        if (repository.existsByMatriculeIgnoreCaseAndIdNot(req.matricule(), id)) {
            throw new ConflictException("Le matricule " + req.matricule() + " est déjà utilisé.");
        }
        if (repository.existsByEmailIgnoreCaseAndIdNot(req.email(), id)) {
            throw new ConflictException("L'email " + req.email() + " est déjà utilisé.");
        }
        apply(e, req);
        audit.log("EMPLOYEE_UPDATED", "Employee", id, e.getMatricule());
        return EmployeeResponse.from(e);
    }

    /** Un employé inactif ou suspendu perd l'accès à l'application. */
    @Transactional
    public EmployeeResponse changeStatus(Long id, EmployeeStatus status) {
        Employee e = load(id);
        EmployeeStatus previous = e.getStatus();
        e.setStatus(status);
        userRepository.findByEmployeeId(id).ifPresent(u -> u.setEnabled(status == EmployeeStatus.ACTIF));
        audit.log("EMPLOYEE_STATUS_CHANGED", "Employee", id, previous + " -> " + status);
        return EmployeeResponse.from(e);
    }

    public Employee load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Employé", id));
    }

    private void apply(Employee e, EmployeeRequest req) {
        if (req.technician() && req.specialty() == null) {
            throw new BusinessException("Un technicien doit avoir une spécialité.");
        }
        e.setMatricule(req.matricule().trim().toUpperCase());
        e.setFirstName(req.firstName().trim());
        e.setLastName(req.lastName().trim());
        e.setEmail(req.email().trim().toLowerCase());
        e.setPhone(req.phone());
        e.setJobTitle(req.jobTitle().trim());
        e.setHireDate(req.hireDate());
        e.setTechnician(req.technician());
        e.setSpecialty(req.technician() ? req.specialty() : null);

        e.setDepartment(req.departmentId() != null ? departmentService.load(req.departmentId()) : null);
        if (req.teamId() != null) {
            Team team = teamService.load(req.teamId());
            if (req.departmentId() != null && !team.getDepartment().getId().equals(req.departmentId())) {
                throw new BusinessException("L'équipe choisie n'appartient pas au département sélectionné.");
            }
            e.setTeam(team);
            e.setDepartment(team.getDepartment());
        } else {
            e.setTeam(null);
        }
    }
}
