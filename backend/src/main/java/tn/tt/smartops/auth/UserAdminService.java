package tn.tt.smartops.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.auth.AuthDtos.*;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.employee.EmployeeRepository;
import tn.tt.smartops.security.CurrentUser;

/** Gestion des comptes applicatifs par l'ADMIN. */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable).map(UserResponse::from));
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Un compte existe déjà avec cet email.");
        }
        User u = new User();
        u.setEmail(req.email().trim().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(req.role());
        Employee e = null;
        if (req.employeeId() != null) {
            e = employeeRepository.findById(req.employeeId())
                    .orElseThrow(() -> new NotFoundException("Employé", req.employeeId()));
            if (userRepository.findByEmployeeId(e.getId()).isPresent()) {
                throw new ConflictException("Cet employé possède déjà un compte.");
            }
        }
        checkRoleMatchesEmployee(req.role(), e);
        u.setEmployee(e);
        userRepository.save(u);
        audit.log("USER_CREATED", "User", u.getId(), u.getEmail() + " / " + u.getRole());
        return UserResponse.from(u);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User u = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur", id));
        if (u.getId().equals(currentUser.get().getId()) && (!req.enabled() || req.role() != Role.ADMIN)) {
            throw new BusinessException("Vous ne pouvez pas retirer vos propres droits d'administration.");
        }
        checkRoleMatchesEmployee(req.role(), u.getEmployee());
        String before = u.getRole() + "/" + u.isEnabled();
        u.setRole(req.role());
        u.setEnabled(req.enabled());
        audit.log("USER_UPDATED", "User", id, before + " -> " + req.role() + "/" + req.enabled());
        return UserResponse.from(u);
    }

    /** Mêmes règles à la création et au changement de rôle. */
    private void checkRoleMatchesEmployee(Role role, Employee e) {
        if (role == Role.CLIENT && e != null) {
            throw new BusinessException("Un compte CLIENT ne peut pas être rattaché à une fiche employé.");
        }
        if (e == null && role != Role.ADMIN && role != Role.CLIENT) {
            throw new BusinessException("Seuls les comptes ADMIN et CLIENT peuvent exister sans fiche employé.");
        }
        if (role == Role.TECHNICIEN && !e.isTechnician()) {
            throw new BusinessException("Le rôle TECHNICIEN exige une fiche employé marquée technicien.");
        }
    }
}
