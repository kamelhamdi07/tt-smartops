package tn.tt.smartops.reclamation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.reclamation.ReclamationDtos.*;

@RestController
@RequestMapping("/api/reclamations")
@RequiredArgsConstructor
public class ReclamationController {

    private final ReclamationService service;

    // ---- client

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENT')")
    public PageResponse<ReclamationResponse> mine(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.mine(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public ReclamationResponse create(@Valid @RequestBody ReclamationRequest req) {
        return service.create(req);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ReclamationResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    // ---- personnel (le filtrage par équipe dirigée est fait dans le service)

    @GetMapping
    @PreAuthorize("!hasRole('CLIENT')")
    public PageResponse<ReclamationResponse> search(@RequestParam(required = false) ReclamationStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.search(status, page, size);
    }

    @GetMapping("/{id}")
    public ReclamationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/options")
    @PreAuthorize("!hasRole('CLIENT')")
    public DispatchOptions options(@PathVariable Long id) {
        return service.options(id);
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("!hasRole('CLIENT')")
    public ReclamationResponse dispatch(@PathVariable Long id, @Valid @RequestBody DispatchRequest req) {
        return service.dispatch(id, req);
    }

    @PatchMapping("/{id}/team")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE')")
    public ReclamationResponse route(@PathVariable Long id, @Valid @RequestBody RouteRequest req) {
        return service.route(id, req);
    }
}
