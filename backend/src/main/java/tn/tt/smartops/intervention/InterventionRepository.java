package tn.tt.smartops.intervention;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {

    @Query(value = "select nextval('intervention_ref_seq')", nativeQuery = true)
    long nextReferenceNumber();

    @EntityGraph(attributePaths = {"team", "equipment", "vehicle", "createdBy"})
    Optional<Intervention> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"team"})
    @Query("""
           select i from Intervention i
           where (:status is null or i.status = :status)
             and (:priority is null or i.priority = :priority)
             and (:teamId is null or i.team.id = :teamId)
             and (:technicianId is null or exists (
                    select 1 from InterventionAssignment a where a.intervention = i and a.technician.id = :technicianId))
             and (cast(:from as LocalDateTime) is null or i.plannedEnd >= :from)
             and (cast(:to as LocalDateTime) is null or i.plannedStart <= :to)
             and (:q is null or lower(i.title) like lower(concat('%', cast(:q as string), '%'))
                  or lower(i.reference) like lower(concat('%', cast(:q as string), '%'))
                  or lower(i.location) like lower(concat('%', cast(:q as string), '%')))
           """)
    Page<Intervention> search(@Param("q") String q, @Param("status") InterventionStatus status,
                              @Param("priority") InterventionPriority priority, @Param("teamId") Long teamId,
                              @Param("technicianId") Long technicianId, @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to, Pageable pageable);

    @Query("""
           select i from Intervention i
           where i.equipment.id = :equipmentId and i.status in :statuses
             and i.plannedStart < :end and i.plannedEnd > :start
             and (:excludeId is null or i.id <> :excludeId)
           """)
    List<Intervention> findEquipmentOverlaps(@Param("equipmentId") Long equipmentId,
                                             @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                             @Param("statuses") java.util.Collection<InterventionStatus> statuses,
                                             @Param("excludeId") Long excludeId);

    @Query("""
           select i from Intervention i
           where i.vehicle.id = :vehicleId and i.status in :statuses
             and i.plannedStart < :end and i.plannedEnd > :start
             and (:excludeId is null or i.id <> :excludeId)
           """)
    List<Intervention> findVehicleOverlaps(@Param("vehicleId") Long vehicleId,
                                           @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                           @Param("statuses") java.util.Collection<InterventionStatus> statuses,
                                           @Param("excludeId") Long excludeId);

    @EntityGraph(attributePaths = {"team", "equipment", "vehicle"})
    @Query("""
           select i from Intervention i
           where i.plannedStart < :to and i.plannedEnd > :from
             and i.status <> tn.tt.smartops.intervention.InterventionStatus.ANNULEE
             and (:teamId is null or i.team.id = :teamId)
           order by i.plannedStart
           """)
    List<Intervention> findScheduledBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                            @Param("teamId") Long teamId);

    long countByStatus(InterventionStatus status);

    @Query("""
           select count(i) from Intervention i
           where i.status in :open and (i.plannedEnd < :now or i.dueDate < :today)
           """)
    long countLate(@Param("open") java.util.Collection<InterventionStatus> open,
                   @Param("now") LocalDateTime now, @Param("today") java.time.LocalDate today);

    @Query("select i.status, count(i) from Intervention i group by i.status")
    List<Object[]> countGroupByStatus();

    @Query("select i.priority, count(i) from Intervention i where i.status <> tn.tt.smartops.intervention.InterventionStatus.ANNULEE group by i.priority")
    List<Object[]> countGroupByPriority();

    @Query("select coalesce(t.name, 'Sans équipe'), count(i) from Intervention i left join i.team t group by t.name")
    List<Object[]> countGroupByTeam();

    @Query(value = """
           select coalesce(avg(extract(epoch from (actual_end - actual_start)) / 60), 0)
           from interventions where actual_start is not null and actual_end is not null
           """, nativeQuery = true)
    double averageDurationMinutes();
}
