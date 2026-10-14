package tn.tt.smartops.equipment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.employee.EmployeeService;
import tn.tt.smartops.equipment.EquipmentDtos.EquipmentRequest;
import tn.tt.smartops.equipment.EquipmentDtos.EquipmentResponse;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository repository;
    private final EmployeeService employeeService;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<EquipmentResponse> search(String q, AssetStatus status, Pageable pageable) {
        return PageResponse.of(repository.search(blankToNull(q), status, pageable).map(EquipmentResponse::from));
    }

    @Transactional(readOnly = true)
    public EquipmentResponse get(Long id) {
        return EquipmentResponse.from(load(id));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest req) {
        if (repository.existsByReferenceIgnoreCase(req.reference())) {
            throw new ConflictException("La référence " + req.reference() + " existe déjà.");
        }
        Equipment e = new Equipment();
        apply(e, req);
        repository.save(e);
        audit.log("EQUIPMENT_CREATED", "Equipment", e.getId(), e.getReference());
        return EquipmentResponse.from(e);
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest req) {
        Equipment e = load(id);
        if (repository.existsByReferenceIgnoreCaseAndIdNot(req.reference(), id)) {
            throw new ConflictException("La référence " + req.reference() + " existe déjà.");
        }
        AssetStatus before = e.getStatus();
        apply(e, req);
        audit.log("EQUIPMENT_UPDATED", "Equipment", id, before == e.getStatus() ? null : before + " -> " + e.getStatus());
        return EquipmentResponse.from(e);
    }

    public Equipment load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Équipement", id));
    }

    private void apply(Equipment e, EquipmentRequest req) {
        e.setReference(req.reference().trim().toUpperCase());
        e.setName(req.name().trim());
        e.setType(req.type().trim());
        e.setStatus(req.status());
        e.setLocation(req.location());
        e.setResponsible(req.responsibleId() != null ? employeeService.load(req.responsibleId()) : null);
        e.setLastMaintenanceDate(req.lastMaintenanceDate());
        e.setNextMaintenanceDate(req.nextMaintenanceDate());
        e.setNotes(req.notes());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
