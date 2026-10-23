package Tenant.Owner.Management.TOM.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Tenant.Owner.Management.TOM.Entity.RentPayment;

public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {

    @Query("SELECT COALESCE(SUM(r.amount + r.fine), 0) FROM RentPayment r " +
            "WHERE r.lease.property.ownerId = :ownerId AND r.status IN :statuses")
    java.math.BigDecimal sumOutstandingByOwnerIdAndStatusIn(@Param("ownerId") Long ownerId,
            @Param("statuses") List<String> statuses);

    boolean existsByLeaseIdAndForMonth(Long leaseId, String forMonth);

    List<RentPayment> findByLeaseIdOrderByDueDateDesc(Long leaseId);

    List<RentPayment> findByLease_TenantIdOrderByDueDateDesc(Long tenantId);

    List<RentPayment> findByLease_TenantIdAndStatusIn(Long tenantId, List<String> statuses);

    Optional<RentPayment> findByIdAndLease_TenantId(Long id, Long tenantId);

    Optional<RentPayment> findByIdAndLease_Property_OwnerId(Long id, Long ownerId);

    List<RentPayment> findByLease_Property_OwnerIdAndStatus(Long ownerId, String status);
}
