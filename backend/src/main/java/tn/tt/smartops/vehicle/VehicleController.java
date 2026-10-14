package tn.tt.smartops.vehicle;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.vehicle.VehicleDtos.VehicleRequest;
import tn.tt.smartops.vehicle.VehicleDtos.VehicleResponse;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE')")
public class VehicleController {

    private final VehicleService service;

    @GetMapping
    public PageResponse<VehicleResponse> search(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) AssetStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.search(q, status, PageRequest.of(page, Math.min(size, 100), Sort.by("plateNumber")));
    }

    @GetMapping("/{id}")
    public VehicleResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody VehicleRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest req) {
        return service.update(id, req);
    }
}
