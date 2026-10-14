package tn.tt.smartops.intervention;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.intervention.InterventionDtos.*;
import tn.tt.smartops.planning.PlanningDtos.TechnicianCandidate;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE')")
    public PageResponse<InterventionListItem> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) InterventionStatus status,
            @RequestParam(required = false) InterventionPriority priority,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return service.search(q, status, priority, teamId, technicianId, from, to, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TECHNICIEN')")
    public PageResponse<InterventionListItem> mine(@RequestParam(required = false) InterventionStatus status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "plannedStart"));
        return service.mine(status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE','TECHNICIEN')")
    public InterventionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE','TECHNICIEN')")
    public List<StatusHistoryResponse> history(@PathVariable Long id) {
        return service.history(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public InterventionResponse create(@Valid @RequestBody InterventionRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public InterventionResponse update(@PathVariable Long id, @Valid @RequestBody InterventionRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/schedule")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public InterventionResponse schedule(@PathVariable Long id, @Valid @RequestBody ScheduleRequest req) {
        return service.schedule(id, req);
    }

    @PutMapping("/{id}/resources")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public InterventionResponse resources(@PathVariable Long id, @RequestBody ResourcesRequest req) {
        return service.setResources(id, req);
    }

    @GetMapping("/{id}/candidates")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public List<TechnicianCandidate> candidates(@PathVariable Long id) {
        return service.candidates(id);
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public InterventionResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        return service.assign(id, req.technicianId());
    }

    @DeleteMapping("/{id}/assignments/{technicianId}")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public InterventionResponse unassign(@PathVariable Long id, @PathVariable Long technicianId) {
        return service.unassign(id, technicianId);
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('TECHNICIEN')")
    public InterventionResponse acknowledge(@PathVariable Long id) {
        return service.acknowledge(id);
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('TECHNICIEN')")
    public InterventionResponse start(@PathVariable Long id) {
        return service.start(id);
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE_TECHNIQUE')")
    public InterventionResponse suspend(@PathVariable Long id, @Valid @RequestBody CommentRequest req) {
        return service.suspend(id, req.comment());
    }

    @PatchMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE_TECHNIQUE')")
    public InterventionResponse resume(@PathVariable Long id) {
        return service.resume(id);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('TECHNICIEN')")
    public InterventionResponse complete(@PathVariable Long id) {
        return service.complete(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public InterventionResponse cancel(@PathVariable Long id, @Valid @RequestBody CommentRequest req) {
        return service.cancel(id, req.comment());
    }
}
