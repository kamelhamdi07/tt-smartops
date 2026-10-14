package tn.tt.smartops.report;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.report.ReportDtos.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @GetMapping("/interventions/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE','TECHNICIEN')")
    public ReportResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/interventions/{id}/report")
    @PreAuthorize("hasRole('TECHNICIEN')")
    public ReportResponse save(@PathVariable Long id, @Valid @RequestBody ReportRequest req) {
        return service.save(id, req);
    }

    @PostMapping("/interventions/{id}/report/submit")
    @PreAuthorize("hasRole('TECHNICIEN')")
    public ReportResponse submit(@PathVariable Long id) {
        return service.submit(id);
    }

    @PostMapping("/interventions/{id}/report/review")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public ReportResponse review(@PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        return service.review(id, req);
    }

    @GetMapping("/reports/pending")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public List<PendingReport> pending() {
        return service.pendingReview();
    }
}
