package tn.tt.smartops.leave;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.leave.LeaveDtos.*;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse create(@Valid @RequestBody LeaveCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/mine")
    public PageResponse<LeaveResponse> mine(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.mine(PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "startDate")));
    }

    @PatchMapping("/{id}/cancel")
    public LeaveResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('RH')")
    public PageResponse<LeaveResponse> search(@RequestParam(required = false) LeaveStatus status,
                                              @RequestParam(required = false) Long employeeId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.search(status, employeeId,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('RH')")
    public LeaveResponse decide(@PathVariable Long id, @Valid @RequestBody LeaveDecisionRequest req) {
        return service.decide(id, req);
    }
}
