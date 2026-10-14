package tn.tt.smartops.reclamation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    @Query(value = "select nextval('reclamation_ref_seq')", nativeQuery = true)
    long nextReferenceNumber();

    @EntityGraph(attributePaths = {"team", "intervention"})
    Page<Reclamation> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "team", "intervention"})
    @Query("""
           select r from Reclamation r
           where (:status is null or r.status = :status)
           order by r.createdAt desc
           """)
    Page<Reclamation> searchAll(@Param("status") ReclamationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "team", "intervention"})
    @Query("""
           select r from Reclamation r
           where r.team.id in :teamIds and (:status is null or r.status = :status)
           order by r.createdAt desc
           """)
    Page<Reclamation> searchTeams(@Param("teamIds") Collection<Long> teamIds,
                                  @Param("status") ReclamationStatus status, Pageable pageable);

    Optional<Reclamation> findByInterventionId(Long interventionId);

    long countByClientIdAndStatusIn(Long clientId, Collection<ReclamationStatus> statuses);

    long countByStatus(ReclamationStatus status);

    long countByTeamIdInAndStatus(Collection<Long> teamIds, ReclamationStatus status);
}
