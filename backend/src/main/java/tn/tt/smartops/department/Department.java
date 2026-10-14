package tn.tt.smartops.department;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "departments")
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;
}
