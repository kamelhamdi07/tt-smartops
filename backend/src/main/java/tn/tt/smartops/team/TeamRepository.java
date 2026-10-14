package tn.tt.smartops.team;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.tt.smartops.reclamation.ReclamationCategory;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = {"department", "leader"})
    List<Team> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"department", "leader"})
    List<Team> findByDepartmentIdOrderByNameAsc(Long departmentId);

    long countByDepartmentId(Long departmentId);

    @Query("select t from Team t join t.categories c where c = :category")
    Optional<Team> findByCategory(@Param("category") ReclamationCategory category);

    List<Team> findByLeaderId(Long employeeId);

    boolean existsByNameIgnoreCaseAndDepartmentId(String name, Long departmentId);
}
