package tn.tt.smartops.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.auth.Role;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.auth.UserRepository;

/**
 * Crée le premier compte ADMIN à partir des variables d'environnement, uniquement
 * si aucun ADMIN n'existe. Aucun identifiant n'est écrit dans le code.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) return;
        String email = props.bootstrap().adminEmail();
        String password = props.bootstrap().adminPassword();
        if (email == null || email.isBlank() || password == null || password.length() < 10) {
            log.warn("Aucun ADMIN en base : définissez BOOTSTRAP_ADMIN_EMAIL et BOOTSTRAP_ADMIN_PASSWORD (10 caractères min).");
            return;
        }
        User admin = new User();
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Compte ADMIN initial créé : {}", admin.getEmail());
    }
}
