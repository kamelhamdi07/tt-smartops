package tn.tt.smartops.department;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.department.DepartmentDtos.DepartmentRequest;
import tn.tt.smartops.department.DepartmentDtos.DepartmentResponse;
import tn.tt.smartops.employee.EmployeeRepository;
import tn.tt.smartops.team.TeamRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository repository;
    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return repository.findAll(Sort.by("name")).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest req) {
        if (repository.existsByCodeIgnoreCase(req.code())) {
            throw new ConflictException("Un département avec le code " + req.code() + " existe déjà.");
        }
        Department d = new Department();
        apply(d, req);
        repository.save(d);
        audit.log("DEPARTMENT_CREATED", "Department", d.getId(), d.getCode());
        return toResponse(d);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest req) {
        Department d = load(id);
        if (repository.existsByCodeIgnoreCaseAndIdNot(req.code(), id)) {
            throw new ConflictException("Un département avec le code " + req.code() + " existe déjà.");
        }
        apply(d, req);
        audit.log("DEPARTMENT_UPDATED", "Department", id, d.getCode());
        return toResponse(d);
    }

    public Department load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Département", id));
    }

    private void apply(Department d, DepartmentRequest req) {
        d.setCode(req.code().trim().toUpperCase());
        d.setName(req.name().trim());
        d.setDescription(req.description());
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getCode(), d.getName(), d.getDescription(),
                teamRepository.countByDepartmentId(d.getId()), employeeRepository.countByDepartmentId(d.getId()));
    }
}
