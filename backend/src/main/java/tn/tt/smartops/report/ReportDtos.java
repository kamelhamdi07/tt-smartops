package tn.tt.smartops.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ReportDtos {
    private ReportDtos() {}

    public record ReportRequest(LocalDateTime workStart, LocalDateTime workEnd,
                                @Size(max = 5000) String problemFound,
                                @Size(max = 5000) String diagnosis,
                                @Size(max = 5000) String solutionApplied,
                                @Size(max = 5000) String materialsUsed,
                                @Size(max = 5000) String replacedParts,
                                @Size(max = 5000) String observations,
                                ReportResult result) {}

    public record ReviewRequest(@NotNull Boolean approve, @Size(max = 1000) String comment) {}

    public record ReportResponse(Long id, Long interventionId, String interventionReference, Long authorId,
                                 String authorName, LocalDateTime workStart, LocalDateTime workEnd,
                                 String problemFound, String diagnosis, String solutionApplied,
                                 String materialsUsed, String replacedParts, String observations,
                                 ReportResult result, ReportStatus status, LocalDateTime submittedAt,
                                 String reviewComment, String reviewedBy, LocalDateTime reviewedAt) {
        public static ReportResponse from(InterventionReport r) {
            return new ReportResponse(r.getId(), r.getIntervention().getId(), r.getIntervention().getReference(),
                    r.getAuthor().getId(), r.getAuthor().getFullName(), r.getWorkStart(), r.getWorkEnd(),
                    r.getProblemFound(), r.getDiagnosis(), r.getSolutionApplied(), r.getMaterialsUsed(),
                    r.getReplacedParts(), r.getObservations(), r.getResult(), r.getStatus(), r.getSubmittedAt(),
                    r.getReviewComment(), r.getReviewedBy() != null ? r.getReviewedBy().getEmail() : null,
                    r.getReviewedAt());
        }
    }

    public record PendingReport(Long reportId, Long interventionId, String interventionReference,
                                String interventionTitle, String authorName, LocalDateTime submittedAt) {}
}
