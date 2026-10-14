package tn.tt.smartops.leave;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("""
           select count(l) > 0 from LeaveRequest l
           where l.employee.id = :employeeId and l.status in :statuses
             and l.startDate <= :end and l.endDate >= :start
             and (:excludeId is null or l.id <> :excludeId)
           """)
    boolean existsOverlap(@Param("employeeId") Long employeeId, @Param("start") LocalDate start,
                          @Param("end") LocalDate end, @Param("statuses") Collection<LeaveStatus> statuses,
                          @Param("excludeId") Long excludeId);

    @Query("""
           select l from LeaveRequest l
           where l.employee.id = :employeeId and l.status = tn.tt.smartops.leave.LeaveStatus.APPROUVEE
             and l.startDate <= :end and l.endDate >= :start
           """)
    List<LeaveRequest> findApprovedOverlapping(@Param("employeeId") Long employeeId,
                                               @Param("start") LocalDate start, @Param("end") LocalDate end);

    @EntityGraph(attributePaths = "employee")
    @Query("""
           select l from LeaveRequest l
           where l.status = tn.tt.smartops.leave.LeaveStatus.APPROUVEE
             and l.startDate <= :end and l.endDate >= :start
           """)
    List<LeaveRequest> findApprovedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @EntityGraph(attributePaths = "employee")
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "processedBy"})
    @Query("""
           select l from LeaveRequest l
           where (:status is null or l.status = :status)
             and (:employeeId is null or l.employee.id = :employeeId)
           """)
    Page<LeaveRequest> search(@Param("status") LeaveStatus status, @Param("employeeId") Long employeeId,
                              Pageable pageable);

    long countByStatus(LeaveStatus status);

    @Query("""
           select count(distinct l.employee.id) from LeaveRequest l
           where l.status = tn.tt.smartops.leave.LeaveStatus.APPROUVEE
             and l.startDate <= :day and l.endDate >= :day
           """)
    long countEmployeesOnLeave(@Param("day") LocalDate day);

    @Query("select l.type, count(l) from LeaveRequest l group by l.type")
    List<Object[]> countByType();
}
