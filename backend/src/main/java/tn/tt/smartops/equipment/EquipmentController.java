package tn.tt.smartops.equipment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.equipment.EquipmentDtos.EquipmentRequest;
import tn.tt.smartops.equipment.EquipmentDtos.EquipmentResponse;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE')")
public class EquipmentController {

    private final EquipmentService service;

    @GetMapping
    public PageResponse<EquipmentResponse> search(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) AssetStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.search(q, status, PageRequest.of(page, Math.min(size, 100), Sort.by("name")));
    }

    @GetMapping("/{id}")
    public EquipmentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse create(@Valid @RequestBody EquipmentRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest req) {
        return service.update(id, req);
    }
}
