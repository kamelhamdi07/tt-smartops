package tn.tt.smartops.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.tt.smartops.security.AppUserDetails;
import tn.tt.smartops.security.CurrentUser;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final CurrentUser currentUser;

    /** Enregistré dans la transaction métier : si l'opération échoue, la trace n'est pas conservée. */
    public void log(String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        var actor = currentUser.find();
        log.setActorId(actor.map(AppUserDetails::getId).orElse(null));
        log.setActorEmail(actor.map(AppUserDetails::getUsername).orElse("système"));
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        repository.save(log);
    }
}
