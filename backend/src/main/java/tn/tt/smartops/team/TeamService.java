package tn.tt.smartops.team;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.department.DepartmentService;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.employee.EmployeeRepository;
import tn.tt.smartops.team.TeamDtos.TeamRequest;
import tn.tt.smartops.team.TeamDtos.TeamResponse;

import tn.tt.smartops.reclamation.ReclamationCategory;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository repository;
    private final DepartmentService departmentService;
    private final EmployeeRepository employeeRepository;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<TeamResponse> findAll(Long departmentId) {
        var teams = departmentId == null ? repository.findAllByOrderByNameAsc()
                : repository.findByDepartmentIdOrderByNameAsc(departmentId);
        return teams.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public TeamResponse create(TeamRequest req) {
        if (repository.existsByNameIgnoreCaseAndDepartmentId(req.name(), req.departmentId())) {
            throw new ConflictException("Cette équipe existe déjà dans ce département.");
        }
        Team t = new Team();
        apply(t, req);
        repository.save(t);
        audit.log("TEAM_CREATED", "Team", t.getId(), t.getName());
        return toResponse(t);
    }

    @Transactional
    public TeamResponse update(Long id, TeamRequest req) {
        Team t = load(id);
        apply(t, req);
        audit.log("TEAM_UPDATED", "Team", id, t.getName());
        return toResponse(t);
    }

    /** Affecte un employé à l'équipe ; son département suit celui de l'équipe. */
    @Transactional
    public TeamResponse addMember(Long teamId, Long employeeId) {
        Team t = load(teamId);
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé", employeeId));
        e.setTeam(t);
        e.setDepartment(t.getDepartment());
        audit.log("TEAM_MEMBER_ADDED", "Team", teamId, "employé " + e.getMatricule());
        return toResponse(t);
    }

    @Transactional
    public TeamResponse removeMember(Long teamId, Long employeeId) {
        Team t = load(teamId);
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé", employeeId));
        if (e.getTeam() == null || !e.getTeam().getId().equals(teamId)) {
            throw new BusinessException("Cet employé n'appartient pas à l'équipe.");
        }
        if (t.getLeader() != null && t.getLeader().getId().equals(employeeId)) {
            t.setLeader(null);
        }
        e.setTeam(null);
        audit.log("TEAM_MEMBER_REMOVED", "Team", teamId, "employé " + e.getMatricule());
        return toResponse(t);
    }

    public Team load(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Équipe", id));
    }

    private void apply(Team t, TeamRequest req) {
        t.setName(req.name().trim());
        t.setDescription(req.description());
        t.setDepartment(departmentService.load(req.departmentId()));
        if (req.leaderId() != null) {
            Employee leader = employeeRepository.findById(req.leaderId())
                    .orElseThrow(() -> new NotFoundException("Employé", req.leaderId()));
            if (leader.getDepartment() == null || !leader.getDepartment().getId().equals(req.departmentId())) {
                throw new BusinessException("Le responsable d'équipe doit appartenir au même département.");
            }
            t.setLeader(leader);
        } else {
            t.setLeader(null);
        }
        var categories = req.categories() == null ? Set.<ReclamationCategory>of() : req.categories();
        for (ReclamationCategory c : categories) {
            repository.findByCategory(c)
                    .filter(other -> !other.getId().equals(t.getId()))
                    .ifPresent(other -> {
                        throw new ConflictException("La catégorie " + c + " est déjà prise en charge par " + other.getName() + ".");
                    });
        }
        t.getCategories().clear();
        t.getCategories().addAll(categories);
    }

    private TeamResponse toResponse(Team t) {
        var leader = t.getLeader();
        return new TeamResponse(t.getId(), t.getName(), t.getDescription(), t.getDepartment().getId(),
                t.getDepartment().getName(), leader != null ? leader.getId() : null,
                leader != null ? leader.getFullName() : null,
                t.getId() == null ? 0 : employeeRepository.countByTeamId(t.getId()),
                new TreeSet<>(t.getCategories()));
    }
}
