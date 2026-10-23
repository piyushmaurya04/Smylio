package Tenant.Owner.Management.TOM.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Tenant.Owner.Management.TOM.Entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    Optional<Notice> findByIdAndOwner_Id(Long id, Long ownerId);

    @Query(value = "SELECT n FROM Notice n LEFT JOIN n.property p " +
            "WHERE n.owner.id = :ownerId " +
            "AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:propertyId IS NULL OR p.id = :propertyId)",
                    countQuery = "SELECT COUNT(n) FROM Notice n LEFT JOIN n.property p " +
                    "WHERE n.owner.id = :ownerId " +
                    "AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:propertyId IS NULL OR p.id = :propertyId)")
    Page<Notice> findByOwnerWithFilters(@Param("ownerId") Long ownerId,
            @Param("search") String search,
            @Param("propertyId") Long propertyId,
            Pageable pageable);

    @Query("SELECT n FROM Notice n " +
            "WHERE n.owner.id IN (SELECT l.property.ownerId FROM Lease l " +
            "WHERE l.tenant.id = :tenantId AND l.status = :leaseStatus) " +
            "AND (n.property IS NULL OR n.property.id IN (SELECT l.property.id FROM Lease l " +
            "WHERE l.tenant.id = :tenantId AND l.status = :leaseStatus)) " +
            "ORDER BY n.createdAt DESC")
    List<Notice> findVisibleToTenant(@Param("tenantId") Long tenantId,
            @Param("leaseStatus") String leaseStatus);

    @Query(value = "SELECT n FROM Notice n LEFT JOIN n.property p " +
            "WHERE n.owner.id IN (SELECT l.property.ownerId FROM Lease l " +
            "WHERE l.tenant.id = :tenantId AND l.status = :leaseStatus) " +
            "AND (n.property IS NULL OR n.property.id IN (SELECT l.property.id FROM Lease l " +
                    "WHERE l.tenant.id = :tenantId AND l.status = :leaseStatus)) " +
                    "AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:propertyId IS NULL OR p.id = :propertyId)",
                    countQuery = "SELECT COUNT(n) FROM Notice n LEFT JOIN n.property p " +
                    "WHERE n.owner.id IN (SELECT l.property.ownerId FROM Lease l " +
                    "WHERE l.tenant.id = :tenantId AND l.status = :leaseStatus) " +
                    "AND (n.property IS NULL OR n.property.id IN (SELECT l.property.id FROM Lease l " +
                    "WHERE l.tenant.id = :tenantId AND l.status = :leaseStatus)) " +
                    "AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:propertyId IS NULL OR p.id = :propertyId)")
    Page<Notice> findVisibleToTenant(@Param("tenantId") Long tenantId,
                    @Param("leaseStatus") String leaseStatus,
                    @Param("search") String search,
                    @Param("propertyId") Long propertyId,
                    Pageable pageable);
}