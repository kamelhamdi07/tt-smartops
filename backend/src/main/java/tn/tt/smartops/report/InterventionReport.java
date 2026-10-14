package tn.tt.smartops.report;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.common.BaseEntity;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.intervention.Intervention;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "intervention_reports")
public class InterventionReport extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intervention_id", unique = true)
    private Intervention intervention;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Employee author;

    @Column(name = "work_start")
    private LocalDateTime workStart;

    @Column(name = "work_end")
    private LocalDateTime workEnd;

    @Column(name = "problem_found", columnDefinition = "TEXT")
    private String problemFound;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "solution_applied", columnDefinition = "TEXT")
    private String solutionApplied;

    @Column(name = "materials_used", columnDefinition = "TEXT")
    private String materialsUsed;

    @Column(name = "replaced_parts", columnDefinition = "TEXT")
    private String replacedParts;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReportResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.BROUILLON;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
