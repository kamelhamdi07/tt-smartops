package tn.tt.smartops.employee;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.employee.EmployeeDtos.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private static final Set<String> SORTABLE = Set.of("lastName", "matricule", "hireDate", "jobTitle");

    private final EmployeeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RH','RESPONSABLE_TECHNIQUE')")
    public PageResponse<EmployeeResponse> search(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) Long departmentId,
                                                 @RequestParam(required = false) Long teamId,
                                                 @RequestParam(required = false) EmployeeStatus status,
                                                 @RequestParam(required = false) Boolean technician,
                                                 @RequestParam(required = false) Specialty specialty,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(defaultValue = "lastName") String sort,
                                                 @RequestParam(defaultValue = "asc") String dir) {
        String sortField = SORTABLE.contains(sort) ? sort : "lastName";
        var pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC), sortField));
        return service.search(q, departmentId, teamId, status, technician, specialty, pageable);
    }

    @GetMapping("/me")
    public EmployeeResponse me() {
        return service.me();
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('ADMIN','RH','RESPONSABLE_TECHNIQUE')")
    public List<EmployeeSummary> technicians() {
        return service.activeTechnicians();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RH','RESPONSABLE_TECHNIQUE')")
    public EmployeeResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RH')")
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RH')")
    public EmployeeResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return service.changeStatus(id, req.status());
    }
}
