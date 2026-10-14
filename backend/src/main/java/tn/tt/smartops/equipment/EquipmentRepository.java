package tn.tt.smartops.equipment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.tt.smartops.common.AssetStatus;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    @EntityGraph(attributePaths = "responsible")
    @Query("""
           select e from Equipment e
           where (:status is null or e.status = :status)
             and (:q is null or lower(e.name) like lower(concat('%', cast(:q as string), '%'))
                  or lower(e.reference) like lower(concat('%', cast(:q as string), '%')))
           """)
    Page<Equipment> search(@Param("q") String q, @Param("status") AssetStatus status, Pageable pageable);

    List<Equipment> findByStatusInOrderByNameAsc(List<AssetStatus> statuses);

    boolean existsByReferenceIgnoreCase(String reference);
    boolean existsByReferenceIgnoreCaseAndIdNot(String reference, Long id);
    long countByStatus(AssetStatus status);
}
