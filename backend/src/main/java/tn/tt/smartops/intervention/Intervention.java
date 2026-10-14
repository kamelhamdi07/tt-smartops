package tn.tt.smartops.intervention;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.common.BaseEntity;
import tn.tt.smartops.employee.Specialty;
import tn.tt.smartops.equipment.Equipment;
import tn.tt.smartops.team.Team;
import tn.tt.smartops.vehicle.Vehicle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "interventions")
public class Intervention extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterventionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterventionPriority priority;

    @Column(nullable = false, length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_specialty", length = 30)
    private Specialty requiredSpecialty;

    @Column(name = "planned_start")
    private LocalDateTime plannedStart;

    @Column(name = "planned_end")
    private LocalDateTime plannedEnd;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterventionStatus status = InterventionStatus.NOUVELLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("assignedAt asc")
    private List<InterventionAssignment> assignments = new ArrayList<>();

    public boolean isScheduled() {
        return plannedStart != null && plannedEnd != null;
    }

    public boolean isLate() {
        boolean open = InterventionStatus.BLOCKING.contains(status) || status == InterventionStatus.NOUVELLE;
        LocalDateTime now = LocalDateTime.now();
        return open && ((plannedEnd != null && plannedEnd.isBefore(now))
                || (dueDate != null && dueDate.isBefore(now.toLocalDate())));
    }
}
