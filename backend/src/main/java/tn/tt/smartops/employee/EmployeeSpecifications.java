package tn.tt.smartops.employee;

import org.springframework.data.jpa.domain.Specification;

/** Filtres combinables pour la recherche d'employés. */
public final class EmployeeSpecifications {
    private EmployeeSpecifications() {}

    public static Specification<Employee> search(String q) {
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("matricule")), like),
                cb.like(cb.lower(root.get("firstName")), like),
                cb.like(cb.lower(root.get("lastName")), like),
                cb.like(cb.lower(root.get("email")), like));
    }

    public static Specification<Employee> department(Long id) {
        return id == null ? null : (root, query, cb) -> cb.equal(root.get("department").get("id"), id);
    }

    public static Specification<Employee> team(Long id) {
        return id == null ? null : (root, query, cb) -> cb.equal(root.get("team").get("id"), id);
    }

    public static Specification<Employee> status(EmployeeStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Employee> technician(Boolean technician) {
        return technician == null ? null : (root, query, cb) -> cb.equal(root.get("technician"), technician);
    }

    public static Specification<Employee> specialty(Specialty specialty) {
        return specialty == null ? null : (root, query, cb) -> cb.equal(root.get("specialty"), specialty);
    }
}
