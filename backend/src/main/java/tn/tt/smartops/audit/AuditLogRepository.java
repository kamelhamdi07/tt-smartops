package tn.tt.smartops.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
           select a from AuditLog a
           where (:entityType is null or a.entityType = :entityType)
             and (:entityId is null or a.entityId = :entityId)
             and (:actor is null or lower(a.actorEmail) like lower(concat('%', cast(:actor as string), '%')))
           """)
    Page<AuditLog> search(@Param("entityType") String entityType, @Param("entityId") Long entityId,
                          @Param("actor") String actor, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    long countByCreatedAtAfter(LocalDateTime since);
}
