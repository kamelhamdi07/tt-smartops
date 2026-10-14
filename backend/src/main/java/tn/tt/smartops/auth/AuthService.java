package tn.tt.smartops.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.audit.AuditService;
import tn.tt.smartops.auth.AuthDtos.*;
import tn.tt.smartops.common.exception.BusinessException;
import tn.tt.smartops.common.exception.ConflictException;
import tn.tt.smartops.security.AppUserDetails;
import tn.tt.smartops.security.CurrentUser;
import tn.tt.smartops.security.JwtService;
import tn.tt.smartops.team.TeamRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final TeamRepository teamRepository;

    @Transactional
    public AuthResponse login(LoginRequest req) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        userRepository.findById(principal.getId()).ifPresent(u -> u.setLastLoginAt(LocalDateTime.now()));
        return new AuthResponse(jwtService.generate(principal), jwtService.getExpirationSeconds(), toInfo(principal));
    }

    /** Crée un compte CLIENT puis ouvre directement sa session. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Un compte existe déjà avec cet email.");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(Role.CLIENT);
        u.setFullName(req.fullName().trim());
        u.setPhone(req.phone().trim());
        u.setAddress(req.address().trim());
        userRepository.saveAndFlush(u);
        audit.log("CLIENT_REGISTERED", "User", u.getId(), email);
        return login(new LoginRequest(email, req.password()));
    }

    public UserInfo me() {
        return toInfo(currentUser.get());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User user = userRepository.findById(currentUser.get().getId()).orElseThrow();
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Le mot de passe actuel est incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        audit.log("PASSWORD_CHANGED", "User", user.getId(), null);
    }

    private UserInfo toInfo(AppUserDetails p) {
        boolean leader = p.getEmployeeId() != null && !teamRepository.findByLeaderId(p.getEmployeeId()).isEmpty();
        return new UserInfo(p.getId(), p.getUsername(), p.getRole(), p.getEmployeeId(), p.getFullName(), leader);
    }
}
