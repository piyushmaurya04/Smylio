package Tenant.Owner.Management.TOM.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Tenant.Owner.Management.TOM.Entity.Lease;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

    boolean existsByPropertyIdAndStatus(Long propertyId, String status);

    List<Lease> findByProperty_OwnerId(Long ownerId);

    Optional<Lease> findByIdAndProperty_OwnerId(Long id, Long ownerId);

    Optional<Lease> findByIdAndTenantId(Long id, Long tenantId);

    List<Lease> findByPropertyId(Long propertyId);

    List<Lease> findByTenantId(Long tenantId);

    List<Lease> findByProperty_OwnerIdAndStatus(Long ownerId, String status);

    boolean existsByPropertyIdAndTenantIdAndStatus(Long propertyId, Long tenantId, String status);

    @Query("SELECT DISTINCT l FROM Lease l " +
            "JOIN FETCH l.property p " +
            "JOIN FETCH l.tenant " +
            "LEFT JOIN FETCH p.amenities " +
            "WHERE l.tenant.id = :tenantId AND p.ownerId = :ownerId " +
            "ORDER BY l.startDate DESC, l.id DESC")
    List<Lease> findOwnerTenantLeasesWithDetails(@Param("tenantId") Long tenantId,
            @Param("ownerId") Long ownerId);

    @Query("SELECT DISTINCT l FROM Lease l " +
            "JOIN FETCH l.property p " +
            "JOIN FETCH l.tenant " +
            "LEFT JOIN FETCH p.amenities " +
            "WHERE p.ownerId = :ownerId")
    List<Lease> findOwnerLeasesWithDetails(Long ownerId);

    @Query("SELECT DISTINCT l FROM Lease l " +
            "JOIN FETCH l.property p " +
            "JOIN FETCH l.tenant " +
            "LEFT JOIN FETCH p.amenities " +
            "WHERE l.tenant.id = :tenantId AND l.status = :status")
    List<Lease> findTenantLeasesWithDetails(@Param("tenantId") Long tenantId,
            @Param("status") String status);

    List<Lease> findByStatus(String status);

    @Query("SELECT COUNT(DISTINCT l.tenant.id) FROM Lease l " +
            "WHERE l.property.ownerId = :ownerId AND l.status = :status")
    long countDistinctTenantsByOwnerIdAndStatus(@Param("ownerId") Long ownerId,
            @Param("status") String status);
}
