package tn.tt.smartops.report;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterventionReportRepository extends JpaRepository<InterventionReport, Long> {

    @EntityGraph(attributePaths = {"author", "reviewedBy"})
    Optional<InterventionReport> findByInterventionId(Long interventionId);

    long countByStatus(ReportStatus status);

    @EntityGraph(attributePaths = {"intervention", "author"})
    List<InterventionReport> findByStatusOrderBySubmittedAtAsc(ReportStatus status);

    @Query("""
           select count(r) from InterventionReport r
           where r.author.id = :employeeId and r.status in :statuses
           """)
    long countForAuthor(@Param("employeeId") Long employeeId,
                        @Param("statuses") java.util.Collection<ReportStatus> statuses);
}
