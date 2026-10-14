package tn.tt.smartops.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.auth.Role;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.common.exception.NotFoundException;
import tn.tt.smartops.common.web.PageResponse;
import tn.tt.smartops.security.CurrentUser;

import java.time.LocalDateTime;

/** Notifications internes. Point d'extension unique si l'on ajoute l'email plus tard. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public record NotificationResponse(Long id, NotificationType type, String title, String message,
                                       String link, boolean read, LocalDateTime createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                    n.getLink(), n.isRead(), n.getCreatedAt());
        }
    }

    public void notifyUser(User recipient, NotificationType type, String title, String message, String link) {
        if (recipient == null || !recipient.isEnabled()) return;
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        repository.save(n);
    }

    /** Notifie le compte rattaché à un employé, s'il existe. */
    public void notifyEmployee(Long employeeId, NotificationType type, String title, String message, String link) {
        userRepository.findByEmployeeId(employeeId)
                .ifPresent(u -> notifyUser(u, type, title, message, link));
    }

    public void notifyRole(Role role, NotificationType type, String title, String message, String link) {
        userRepository.findByRoleAndEnabledTrue(role)
                .forEach(u -> notifyUser(u, type, title, message, link));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> mine(int page, int size) {
        Long userId = currentUser.get().getId();
        return PageResponse.of(repository.findByRecipientIdOrderByCreatedAtDesc(userId,
                PageRequest.of(page, Math.min(size, 50))).map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByRecipientIdAndReadFalse(currentUser.get().getId());
    }

    @Transactional
    public void markRead(Long id) {
        Notification n = repository.findById(id).orElseThrow(() -> new NotFoundException("Notification", id));
        // Un utilisateur ne peut agir que sur ses propres notifications
        if (!n.getRecipient().getId().equals(currentUser.get().getId())) {
            throw new NotFoundException("Notification", id);
        }
        n.setRead(true);
    }

    @Transactional
    public void markAllRead() {
        repository.markAllRead(currentUser.get().getId());
    }
}
