package tn.tt.smartops.planning;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.tt.smartops.employee.EmployeeService;
import tn.tt.smartops.planning.PlanningDtos.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningService planningService;
    private final AvailabilityService availabilityService;
    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE')")
    public List<PlanningEntry> entries(@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime from,
                                       @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime to,
                                       @RequestParam(required = false) Long teamId,
                                       @RequestParam(required = false) Long technicianId) {
        return planningService.entries(from, to, teamId, technicianId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TECHNICIEN')")
    public List<PlanningEntry> mine(@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime from,
                                    @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime to) {
        return planningService.myEntries(from, to);
    }

    @GetMapping("/conflicts")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE_TECHNIQUE')")
    public List<PlanningConflict> conflicts(@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime from,
                                            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime to) {
        return planningService.detectConflicts(from, to);
    }

    @GetMapping("/technicians/{id}/availability")
    @PreAuthorize("hasAnyRole('RH','RESPONSABLE_TECHNIQUE')")
    public List<AvailabilitySlot> availability(@PathVariable Long id,
                                               @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return availabilityService.dayTimeline(employeeService.load(id), date);
    }

    @GetMapping("/resources/available")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    public AvailableResources availableResources(@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime start,
                                                 @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime end,
                                                 @RequestParam(required = false) Long excludeInterventionId) {
        return planningService.availableResources(start, end, excludeInterventionId);
    }
}
