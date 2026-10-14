package tn.tt.smartops.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.common.BaseEntity;
import tn.tt.smartops.employee.Employee;

import java.time.LocalDateTime;

/** Compte applicatif. Un compte peut être rattaché à une fiche employé (sauf compte technique ADMIN). */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", unique = true)
    private Employee employee;

    /** Coordonnées des comptes CLIENT, qui n'ont pas de fiche employé. */
    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
