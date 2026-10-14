package tn.tt.smartops.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tn.tt.smartops.auth.Role;
import tn.tt.smartops.auth.User;

import java.util.Collection;
import java.util.List;

/** Principal de sécurité : ne transporte que ce qui est nécessaire aux contrôles d'accès. */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;
    private final Long employeeId;
    private final String fullName;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;
        this.fullName = user.getEmployee() != null ? user.getEmployee().getFullName()
                : user.getFullName() != null ? user.getFullName() : user.getEmail();
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public Long getEmployeeId() { return employeeId; }
    public String getFullName() { return fullName; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
