package tn.tt.smartops.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.tt.smartops.reclamation.ReclamationCategory;

import java.util.Set;

public final class TeamDtos {
    private TeamDtos() {}

    public record TeamRequest(@NotBlank @Size(max = 120) String name,
                              @Size(max = 500) String description,
                              @NotNull Long departmentId,
                              Long leaderId,
                              Set<ReclamationCategory> categories) {}

    public record TeamResponse(Long id, String name, String description, Long departmentId, String departmentName,
                               Long leaderId, String leaderName, long memberCount,
                               Set<ReclamationCategory> categories) {}
}
