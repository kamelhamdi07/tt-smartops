package tn.tt.smartops.absence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    @Query("""
           select a from Absence a
           where a.employee.id = :employeeId and a.startDate <= :end and a.endDate >= :start
             and (:excludeId is null or a.id <> :excludeId)
           """)
    List<Absence> findOverlapping(@Param("employeeId") Long employeeId, @Param("start") LocalDate start,
                                  @Param("end") LocalDate end, @Param("excludeId") Long excludeId);

    @EntityGraph(attributePaths = "employee")
    @Query("select a from Absence a where a.startDate <= :end and a.endDate >= :start")
    List<Absence> findBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @EntityGraph(attributePaths = {"employee", "recordedBy"})
    @Query("""
           select a from Absence a
           where (:employeeId is null or a.employee.id = :employeeId)
             and (cast(:from as LocalDate) is null or a.endDate >= :from)
             and (cast(:to as LocalDate) is null or a.startDate <= :to)
           """)
    Page<Absence> search(@Param("employeeId") Long employeeId, @Param("from") LocalDate from,
                         @Param("to") LocalDate to, Pageable pageable);

    @Query("""
           select count(distinct a.employee.id) from Absence a
           where a.startDate <= :day and a.endDate >= :day
           """)
    long countEmployeesAbsent(@Param("day") LocalDate day);
}
