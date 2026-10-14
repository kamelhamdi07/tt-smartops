package tn.tt.smartops.team;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.common.BaseEntity;
import tn.tt.smartops.department.Department;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.reclamation.ReclamationCategory;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "teams")
public class Team extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private Employee leader;

    /** Catégories de réclamations client routées vers cette équipe (une catégorie = une seule équipe). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "team_reclamation_categories", joinColumns = @JoinColumn(name = "team_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private Set<ReclamationCategory> categories = new HashSet<>();
}
