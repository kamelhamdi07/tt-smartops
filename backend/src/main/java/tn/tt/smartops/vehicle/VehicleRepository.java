package tn.tt.smartops.vehicle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.tt.smartops.common.AssetStatus;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @EntityGraph(attributePaths = "team")
    @Query("""
           select v from Vehicle v
           where (:status is null or v.status = :status)
             and (:q is null or lower(v.plateNumber) like lower(concat('%', cast(:q as string), '%'))
                  or lower(v.brandModel) like lower(concat('%', cast(:q as string), '%')))
           """)
    Page<Vehicle> search(@Param("q") String q, @Param("status") AssetStatus status, Pageable pageable);

    List<Vehicle> findByStatusInOrderByPlateNumberAsc(List<AssetStatus> statuses);

    boolean existsByPlateNumberIgnoreCase(String plateNumber);
    boolean existsByPlateNumberIgnoreCaseAndIdNot(String plateNumber, Long id);
    long countByStatus(AssetStatus status);
}
