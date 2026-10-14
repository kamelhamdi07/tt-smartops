package tn.tt.smartops.employee;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class EmployeeDtos {
    private EmployeeDtos() {}

    public record EmployeeRequest(
            @NotBlank @Size(max = 20) String matricule,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 150) String email,
            @Pattern(regexp = "^[+0-9 ]{8,20}$", message = "Numéro de téléphone invalide.") String phone,
            @NotBlank @Size(max = 120) String jobTitle,
            @NotNull @PastOrPresent LocalDate hireDate,
            boolean technician,
            Specialty specialty,
            Long departmentId,
            Long teamId) {}

    public record StatusRequest(@NotNull EmployeeStatus status) {}

    public record EmployeeResponse(Long id, String matricule, String firstName, String lastName, String fullName,
                                   String email, String phone, String jobTitle, LocalDate hireDate,
                                   EmployeeStatus status, boolean technician, Specialty specialty,
                                   Long departmentId, String departmentName, Long teamId, String teamName) {
        public static EmployeeResponse from(Employee e) {
            var d = e.getDepartment();
            var t = e.getTeam();
            return new EmployeeResponse(e.getId(), e.getMatricule(), e.getFirstName(), e.getLastName(),
                    e.getFullName(), e.getEmail(), e.getPhone(), e.getJobTitle(), e.getHireDate(), e.getStatus(),
                    e.isTechnician(), e.getSpecialty(),
                    d != null ? d.getId() : null, d != null ? d.getName() : null,
                    t != null ? t.getId() : null, t != null ? t.getName() : null);
        }
    }

    /** Vue réduite, utilisée dans les listes déroulantes et les affectations. */
    public record EmployeeSummary(Long id, String matricule, String fullName, Specialty specialty,
                                  Long teamId, String teamName) {
        public static EmployeeSummary from(Employee e) {
            var t = e.getTeam();
            return new EmployeeSummary(e.getId(), e.getMatricule(), e.getFullName(), e.getSpecialty(),
                    t != null ? t.getId() : null, t != null ? t.getName() : null);
        }
    }
}
