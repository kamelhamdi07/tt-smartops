package tn.tt.smartops.intervention;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface InterventionAssignmentRepository extends JpaRepository<InterventionAssignment, Long> {

    @Query("""
           select a from InterventionAssignment a join fetch a.technician
           where a.intervention.id = :interventionId order by a.assignedAt
           """)
    List<InterventionAssignment> findByInterventionId(@Param("interventionId") Long interventionId);

    @Query("""
           select a from InterventionAssignment a join fetch a.technician
           where a.intervention.id in :ids
           """)
    List<InterventionAssignment> findByInterventionIds(@Param("ids") Collection<Long> ids);

    boolean existsByInterventionIdAndTechnicianId(Long interventionId, Long technicianId);

    /** Interventions bloquantes d'un technicien qui chevauchent le créneau [start, end[. */
    @Query("""
           select a from InterventionAssignment a join fetch a.intervention i
           where a.technician.id = :technicianId and i.status in :statuses
             and i.plannedStart < :end and i.plannedEnd > :start
             and (:excludeId is null or i.id <> :excludeId)
           order by i.plannedStart
           """)
    List<InterventionAssignment> findTechnicianOverlaps(@Param("technicianId") Long technicianId,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end,
                                                        @Param("statuses") Collection<InterventionStatus> statuses,
                                                        @Param("excludeId") Long excludeId);

    @Query("""
           select a from InterventionAssignment a join fetch a.intervention i join fetch a.technician
           where i.status in :statuses and i.plannedStart < :to and i.plannedEnd > :from
           """)
    List<InterventionAssignment> findActiveBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                                   @Param("statuses") Collection<InterventionStatus> statuses);

    @Query("""
           select count(a) from InterventionAssignment a
           where a.technician.id = :technicianId and a.intervention.status in :statuses
           """)
    long countForTechnician(@Param("technicianId") Long technicianId,
                            @Param("statuses") Collection<InterventionStatus> statuses);

    @Query("""
           select a.technician.firstName || ' ' || a.technician.lastName, count(a)
           from InterventionAssignment a
           where a.intervention.status <> tn.tt.smartops.intervention.InterventionStatus.ANNULEE
           group by a.technician.id, a.technician.firstName, a.technician.lastName
           order by count(a) desc
           """)
    List<Object[]> countGroupByTechnician();
}
