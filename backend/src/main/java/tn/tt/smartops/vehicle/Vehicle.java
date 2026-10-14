package tn.tt.smartops.vehicle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.BaseEntity;
import tn.tt.smartops.team.Team;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Column(name = "brand_model", nullable = false, length = 100)
    private String brandModel;

    @Column(nullable = false, length = 40)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status = AssetStatus.DISPONIBLE;

    @Column(nullable = false)
    private int mileage;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
