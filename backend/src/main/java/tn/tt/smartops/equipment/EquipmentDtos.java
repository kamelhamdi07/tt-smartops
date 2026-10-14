package tn.tt.smartops.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.tt.smartops.common.AssetStatus;

import java.time.LocalDate;

public final class EquipmentDtos {
    private EquipmentDtos() {}

    public record EquipmentRequest(@NotBlank @Size(max = 40) String reference,
                                   @NotBlank @Size(max = 150) String name,
                                   @NotBlank @Size(max = 60) String type,
                                   @NotNull AssetStatus status,
                                   @Size(max = 150) String location,
                                   Long responsibleId,
                                   LocalDate lastMaintenanceDate,
                                   LocalDate nextMaintenanceDate,
                                   @Size(max = 1000) String notes) {}

    public record EquipmentResponse(Long id, String reference, String name, String type, AssetStatus status,
                                    String location, Long responsibleId, String responsibleName,
                                    LocalDate lastMaintenanceDate, LocalDate nextMaintenanceDate, String notes) {
        public static EquipmentResponse from(Equipment e) {
            var r = e.getResponsible();
            return new EquipmentResponse(e.getId(), e.getReference(), e.getName(), e.getType(), e.getStatus(),
                    e.getLocation(), r != null ? r.getId() : null, r != null ? r.getFullName() : null,
                    e.getLastMaintenanceDate(), e.getNextMaintenanceDate(), e.getNotes());
        }
    }
}
