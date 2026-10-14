package tn.tt.smartops.reclamation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.tt.smartops.employee.Specialty;
import tn.tt.smartops.intervention.InterventionPriority;

import java.time.LocalDateTime;
import java.util.List;

public final class ReclamationDtos {
    private ReclamationDtos() {}

    /** Adresse et téléphone sont facultatifs : à défaut, ceux du profil client sont repris. */
    public record ReclamationRequest(@NotNull ReclamationCategory category,
                                     @NotBlank @Size(max = 200) String title,
                                     @NotBlank @Size(max = 2000) String description,
                                     @Size(max = 255) String address,
                                     @Size(max = 30) String phone) {}

    /** Décision du chef d'équipe : technicien, véhicule et créneau. */
    public record DispatchRequest(@NotNull Long technicianId,
                                  Long vehicleId,
                                  @NotNull LocalDateTime plannedStart,
                                  @NotNull LocalDateTime plannedEnd,
                                  @NotNull InterventionPriority priority) {}

    public record RouteRequest(@NotNull Long teamId) {}

    public record ReclamationResponse(Long id, String reference, ReclamationCategory category, String title,
                                      String description, String address, String phone, ReclamationStatus status,
                                      String clientName, String clientEmail, Long teamId, String teamName,
                                      Long interventionId, String interventionReference, List<String> technicians,
                                      String vehiclePlate, LocalDateTime plannedStart, LocalDateTime plannedEnd,
                                      LocalDateTime createdAt, LocalDateTime resolvedAt, boolean canDispatch) {}

    public record TechnicianOption(Long id, String fullName, Specialty specialty) {}

    public record VehicleOption(Long id, String plateNumber, String brandModel, String teamName) {}

    public record DispatchOptions(List<TechnicianOption> technicians, List<VehicleOption> vehicles) {}
}
