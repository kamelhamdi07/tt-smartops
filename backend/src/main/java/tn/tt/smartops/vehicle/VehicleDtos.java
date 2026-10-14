package tn.tt.smartops.vehicle;

import jakarta.validation.constraints.*;
import tn.tt.smartops.common.AssetStatus;

import java.time.LocalDate;

public final class VehicleDtos {
    private VehicleDtos() {}

    public record VehicleRequest(@NotBlank @Size(max = 20) String plateNumber,
                                 @NotBlank @Size(max = 100) String brandModel,
                                 @NotBlank @Size(max = 40) String type,
                                 @NotNull AssetStatus status,
                                 @PositiveOrZero int mileage,
                                 LocalDate lastMaintenanceDate,
                                 Long teamId) {}

    public record VehicleResponse(Long id, String plateNumber, String brandModel, String type, AssetStatus status,
                                  int mileage, LocalDate lastMaintenanceDate, Long teamId, String teamName) {
        public static VehicleResponse from(Vehicle v) {
            var t = v.getTeam();
            return new VehicleResponse(v.getId(), v.getPlateNumber(), v.getBrandModel(), v.getType(), v.getStatus(),
                    v.getMileage(), v.getLastMaintenanceDate(), t != null ? t.getId() : null,
                    t != null ? t.getName() : null);
        }
    }
}
