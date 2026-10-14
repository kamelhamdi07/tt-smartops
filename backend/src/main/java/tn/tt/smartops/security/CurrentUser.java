package tn.tt.smartops.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tn.tt.smartops.auth.Role;
import tn.tt.smartops.common.exception.BusinessException;

import java.util.Optional;

/** Accès centralisé à l'utilisateur connecté, utilisé par les services pour les contrôles de propriété. */
@Component
public class CurrentUser {

    public AppUserDetails get() {
        return find().orElseThrow(() -> new AccessDeniedException("Non authentifié"));
    }

    public Optional<AppUserDetails> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }

    public Long requireEmployeeId() {
        Long id = get().getEmployeeId();
        if (id == null) {
            throw new BusinessException("Ce compte n'est rattaché à aucune fiche employé.");
        }
        return id;
    }

    public boolean hasRole(Role... roles) {
        Role current = get().getRole();
        for (Role r : roles) {
            if (r == current) return true;
        }
        return false;
    }

    public String displayName() {
        return find().map(AppUserDetails::getUsername).orElse("système");
    }
}
