package tn.tt.smartops.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    /** teamLeader : l'employé dirige au moins une équipe et traite donc ses réclamations clients. */
    public record UserInfo(Long id, String email, Role role, Long employeeId, String fullName, boolean teamLeader) {}

    public record AuthResponse(String token, long expiresInSeconds, UserInfo user) {}

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 10, message = "Le mot de passe doit contenir au moins 10 caractères.") String password,
            @NotNull Role role,
            Long employeeId) {}

    /** Inscription libre d'un client (rôle CLIENT, sans fiche employé). */
    public record RegisterRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 30) String phone,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(min = 10, message = "Le mot de passe doit contenir au moins 10 caractères.") String password) {}

    public record UpdateUserRequest(@NotNull Role role, boolean enabled) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 10) String newPassword) {}

    public record UserResponse(Long id, String email, Role role, boolean enabled, Long employeeId,
                               String employeeName, LocalDateTime lastLoginAt, LocalDateTime createdAt) {
        public static UserResponse from(User u) {
            var e = u.getEmployee();
            return new UserResponse(u.getId(), u.getEmail(), u.getRole(), u.isEnabled(),
                    e != null ? e.getId() : null, e != null ? e.getFullName() : null,
                    u.getLastLoginAt(), u.getCreatedAt());
        }
    }
}
