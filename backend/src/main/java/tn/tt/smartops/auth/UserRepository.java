package tn.tt.smartops.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "employee")
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    Optional<User> findByEmployeeId(Long employeeId);

    List<User> findByRoleAndEnabledTrue(Role role);

    @EntityGraph(attributePaths = "employee")
    Page<User> findAll(Pageable pageable);

    long countByEnabledTrue();
}
