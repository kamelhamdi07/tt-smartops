package tn.tt.smartops.intervention;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterventionStatusHistoryRepository extends JpaRepository<InterventionStatusHistory, Long> {
    List<InterventionStatusHistory> findByInterventionIdOrderByChangedAtAsc(Long interventionId);
}
