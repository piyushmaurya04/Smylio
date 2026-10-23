package Tenant.Owner.Management.TOM.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import Tenant.Owner.Management.TOM.Entity.MaintenanceRequest;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    long countByProperty_OwnerIdAndStatusIn(Long ownerId, List<String> statuses);

    long countByTenantIdAndProperty_OwnerId(Long tenantId, Long ownerId);

    long countByTenantIdAndPropertyIdAndProperty_OwnerId(Long tenantId, Long propertyId, Long ownerId);

    List<MaintenanceRequest> findByTenantId(Long tenantId);

    List<MaintenanceRequest> findByPropertyIdAndTenantId(Long propertyId, Long tenantId);

    List<MaintenanceRequest> findByPropertyIdAndProperty_OwnerId(Long propertyId, Long ownerId);

    Optional<MaintenanceRequest> findByIdAndProperty_OwnerId(Long id, Long ownerId);

    Optional<MaintenanceRequest> findByIdAndTenantId(Long id, Long tenantId);

    @Query(value = "SELECT m FROM MaintenanceRequest m " +
            "WHERE m.property.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.property.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.tenant.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:propertyId IS NULL OR m.property.id = :propertyId)",
            countQuery = "SELECT COUNT(m) FROM MaintenanceRequest m " +
                    "WHERE m.property.ownerId = :ownerId " +
                    "AND (:search IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(m.property.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(m.tenant.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:propertyId IS NULL OR m.property.id = :propertyId)")
    Page<MaintenanceRequest> findOwnerRequests(@Param("ownerId") Long ownerId,
            @Param("search") String search,
            @Param("propertyId") Long propertyId,
            Pageable pageable);

    @Query(value = "SELECT m FROM MaintenanceRequest m " +
            "WHERE m.tenant.id = :tenantId " +
            "AND (:search IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.property.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:propertyId IS NULL OR m.property.id = :propertyId)",
            countQuery = "SELECT COUNT(m) FROM MaintenanceRequest m " +
                    "WHERE m.tenant.id = :tenantId " +
                    "AND (:search IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(m.property.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:propertyId IS NULL OR m.property.id = :propertyId)")
    Page<MaintenanceRequest> findTenantRequests(@Param("tenantId") Long tenantId,
            @Param("search") String search,
            @Param("propertyId") Long propertyId,
            Pageable pageable);
}