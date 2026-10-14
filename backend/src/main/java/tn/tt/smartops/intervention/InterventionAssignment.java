package tn.tt.smartops.intervention;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.employee.Employee;

import java.time.LocalDateTime;

/** Affectation d'un technicien à une intervention (plusieurs techniciens possibles). */
@Getter
@Setter
@Entity
@Table(name = "intervention_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"intervention_id", "technician_id"}))
public class InterventionAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intervention_id")
    private Intervention intervention;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id")
    private Employee technician;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    /** Date de prise de connaissance par le technicien. */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
}
