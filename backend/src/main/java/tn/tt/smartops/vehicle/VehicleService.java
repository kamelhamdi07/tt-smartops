package tn.tt.smartops.vehicle;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.team.TeamService;
import tn.tt.smartops.vehicle.VehicleDtos.VehicleRequest;
import tn.tt.smartops.vehicle.VehicleDtos.VehicleResponse;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository repository;
    private final TeamService teamService;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> search(String q, AssetStatus status, Pageable pageable) {
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(repository.search(query, status, pageable).map(VehicleResponse::from));
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(Long id) {
        return VehicleResponse.from(load(id));
    }

    @Transactional
    public VehicleResponse create(VehicleRequest req) {
        if (repository.existsByPlateNumberIgnoreCase(req.plateNumber())) {
            throw new ConflictException("Le véhicule " + req.plateNumber() + " existe déjà.");
        }
        Vehicle v = new Vehicle();
        apply(v, req);
        repository.save(v);
        audit.log("VEHICLE_CREATED", "Vehicle", v.getId(), v.getPlateNumber());
        return VehicleResponse.from(v);
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest req) {
        Vehicle v = load(id);
        if (repository.existsByPlateNumberIgnoreCaseAndIdNot(req.plateNumber(), id)) {
            throw new ConflictException("Le véhicule " + req.plateNumber() + " existe déjà.");
        }
        if (req.mileage() < v.getMileage()) {
            throw new BusinessException("Le kilométrage ne peut pas diminuer.");
        }
        apply(v, req);
        audit.log("VEHICLE_UPDATED", "Vehicle", id, v.getPlateNumber());
        return VehicleResponse.from(v);
    }

    public Vehicle load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Véhicule", id));
    }

    private void apply(Vehicle v, VehicleRequest req) {
        v.setPlateNumber(req.plateNumber().trim().toUpperCase());
        v.setBrandModel(req.brandModel().trim());
        v.setType(req.type().trim());
        v.setStatus(req.status());
        v.setMileage(req.mileage());
        v.setLastMaintenanceDate(req.lastMaintenanceDate());
        v.setTeam(req.teamId() != null ? teamService.load(req.teamId()) : null);
    }
}
