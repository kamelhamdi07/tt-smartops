package tn.tt.smartops.team;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.team.TeamDtos.TeamRequest;
import tn.tt.smartops.team.TeamDtos.TeamResponse;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RH','RESPONSABLE_TECHNIQUE')")
    public List<TeamResponse> list(@RequestParam(required = false) Long departmentId) {
        return service.findAll(departmentId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RH','RESPONSABLE_TECHNIQUE')")
    public TeamResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public TeamResponse create(@Valid @RequestBody TeamRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public TeamResponse update(@PathVariable Long id, @Valid @RequestBody TeamRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/{id}/members/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public TeamResponse addMember(@PathVariable Long id, @PathVariable Long employeeId) {
        return service.addMember(id, employeeId);
    }

    @DeleteMapping("/{id}/members/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public TeamResponse removeMember(@PathVariable Long id, @PathVariable Long employeeId) {
        return service.removeMember(id, employeeId);
    }
}
