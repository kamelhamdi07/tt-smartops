package tn.tt.smartops.absence;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.absence.AbsenceDtos.AbsenceRequest;
import tn.tt.smartops.absence.AbsenceDtos.AbsenceResponse;
import tn.tt.smartops.common.web.PageResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
public class AbsenceController {

    private final AbsenceService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('RH','RESPONSABLE_TECHNIQUE')")
    public PageResponse<AbsenceResponse> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(employeeId, from, to,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "startDate")));
    }

    @GetMapping("/mine")
    public PageResponse<AbsenceResponse> mine(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.mine(PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "startDate")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RH')")
    public AbsenceResponse create(@Valid @RequestBody AbsenceRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public AbsenceResponse update(@PathVariable Long id, @Valid @RequestBody AbsenceRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
