package tn.tt.smartops.absence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.common.BaseEntity;
import tn.tt.smartops.employee.Employee;

import java.time.LocalDate;

/** Absence constatée par le service RH ; rend l'employé indisponible sur la période. */
@Getter
@Setter
@Entity
@Table(name = "absences")
public class Absence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AbsenceType type;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean justified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
}
