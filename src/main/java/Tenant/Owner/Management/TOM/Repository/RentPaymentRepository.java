package Tenant.Owner.Management.TOM.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Tenant.Owner.Management.TOM.Constants.PaymentMethod;
import Tenant.Owner.Management.TOM.Constants.RentStatus;
import Tenant.Owner.Management.TOM.Entity.RentPayment;

public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {

    @Query("SELECT COALESCE(SUM(r.amount + r.fine), 0) FROM RentPayment r " +
            "WHERE r.lease.property.ownerId = :ownerId AND r.status IN :statuses")
    java.math.BigDecimal sumOutstandingByOwnerIdAndStatusIn(@Param("ownerId") Long ownerId,
            @Param("statuses") List<RentStatus> statuses);

    boolean existsByLeaseIdAndForMonth(Long leaseId, String forMonth);

    List<RentPayment> findByLeaseIdOrderByDueDateDesc(Long leaseId);

    List<RentPayment> findByLease_TenantIdOrderByDueDateDesc(Long tenantId);

    List<RentPayment> findByLease_TenantIdAndStatusIn(Long tenantId, List<RentStatus> statuses);

    Optional<RentPayment> findByIdAndLease_TenantId(Long id, Long tenantId);

    Optional<RentPayment> findByIdAndLease_Property_OwnerId(Long id, Long ownerId);

    List<RentPayment> findByLease_Property_OwnerIdAndStatus(Long ownerId, RentStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RentPayment r SET r.status = :paidStatus, r.method = :method, " +
            "r.transactionId = :transactionId, r.paidDate = :paidDate, r.fine = :fine " +
            "WHERE r.id = :id AND r.status IN :allowedStatuses " +
            "AND r.lease.id IN (SELECT l.id FROM Lease l WHERE l.tenant.id = :tenantId)")
    int markPaidOnlineIfPayable(@Param("id") Long id,
            @Param("tenantId") Long tenantId,
            @Param("paidStatus") RentStatus paidStatus,
            @Param("method") PaymentMethod method,
            @Param("transactionId") String transactionId,
            @Param("paidDate") LocalDate paidDate,
            @Param("fine") BigDecimal fine,
            @Param("allowedStatuses") List<RentStatus> allowedStatuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RentPayment r SET r.status = :awaitingStatus, r.method = :method, r.fine = :fine " +
            "WHERE r.id = :id AND r.status IN :allowedStatuses " +
            "AND r.lease.id IN (SELECT l.id FROM Lease l WHERE l.tenant.id = :tenantId)")
    int markCashClaimIfPayable(@Param("id") Long id,
            @Param("tenantId") Long tenantId,
            @Param("awaitingStatus") RentStatus awaitingStatus,
            @Param("method") PaymentMethod method,
            @Param("fine") BigDecimal fine,
            @Param("allowedStatuses") List<RentStatus> allowedStatuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RentPayment r SET r.status = :paidStatus, r.paidDate = :paidDate " +
            "WHERE r.id = :id AND r.status = :awaitingStatus " +
            "AND r.lease.id IN (SELECT l.id FROM Lease l WHERE l.property.ownerId = :ownerId)")
    int approveCashIfAwaiting(@Param("id") Long id,
            @Param("ownerId") Long ownerId,
            @Param("paidStatus") RentStatus paidStatus,
            @Param("paidDate") LocalDate paidDate,
            @Param("awaitingStatus") RentStatus awaitingStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RentPayment r SET r.status = :nextStatus, r.fine = :fine, r.method = null " +
            "WHERE r.id = :id AND r.status = :awaitingStatus " +
            "AND r.lease.id IN (SELECT l.id FROM Lease l WHERE l.property.ownerId = :ownerId)")
    int rejectCashIfAwaiting(@Param("id") Long id,
            @Param("ownerId") Long ownerId,
            @Param("nextStatus") RentStatus nextStatus,
            @Param("fine") BigDecimal fine,
            @Param("awaitingStatus") RentStatus awaitingStatus);
}
