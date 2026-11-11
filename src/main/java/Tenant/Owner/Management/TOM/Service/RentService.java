package Tenant.Owner.Management.TOM.Service;

import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.RentPayment;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import Tenant.Owner.Management.TOM.Constants.LeaseStatus;
import Tenant.Owner.Management.TOM.Constants.PaymentMethod;
import Tenant.Owner.Management.TOM.Constants.RentStatus;
import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.DTO.DuesResponse;
import Tenant.Owner.Management.TOM.DTO.RentResponse;
import Tenant.Owner.Management.TOM.DTO.TenantRentsResponse;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.RentPaymentRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RentService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<RentStatus> PAYABLE_STATUSES = List.of(
            RentStatus.PENDING, RentStatus.LATE);

    private final RentPaymentRepository rentPaymentRepository;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public RentService(RentPaymentRepository rentPaymentRepository, LeaseRepository leaseRepository,
            UserRepository userRepository, Clock clock) {
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public int generateRentForLease(Lease lease) {
        return generateCompletedCyclesUpTo(lease, LocalDate.now(clock));
    }

    @Transactional
    public int generateFinalRentOnLeaseEnd(Lease lease, LocalDate endDate) {
        LocalDate start = lease.getStartDate();
        if (!endDate.isAfter(start)) {
            applyFinesForLease(lease);
            return 0;
        }

        int generated = generateCompletedCyclesUpTo(lease, endDate);

        int k = 1;
        while (!start.plusMonths(k).isAfter(endDate)) {
            k++;
        }
        LocalDate cycleStart = start.plusMonths(k - 1);
        LocalDate cycleEnd = start.plusMonths(k);

        if (endDate.isAfter(cycleStart)) {
            String forMonth = cycleStart.format(MONTH_FMT);
            if (!rentPaymentRepository.existsByLeaseIdAndForMonth(lease.getId(), forMonth)) {
                long daysOccupied = ChronoUnit.DAYS.between(cycleStart, endDate);
                long daysInCycle = ChronoUnit.DAYS.between(cycleStart, cycleEnd);
                BigDecimal proratedAmount = lease.getMonthlyRent()
                        .multiply(BigDecimal.valueOf(daysOccupied))
                        .divide(BigDecimal.valueOf(daysInCycle), 2, RoundingMode.HALF_UP);

                RentPayment rent = new RentPayment();
                rent.setLease(lease);
                rent.setForMonth(forMonth);
                rent.setAmount(proratedAmount);
                rent.setDueDate(endDate);
                rent.setFine(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                rent.setStatus(RentStatus.PENDING);
                rentPaymentRepository.save(rent);
                generated++;
            }
        }

        applyFinesForLease(lease);
        return generated;
    }

    private int generateCompletedCyclesUpTo(Lease lease, LocalDate upToDate) {
        LocalDate start = lease.getStartDate();

        int k = 1;
        int generated = 0;
        while (!start.plusMonths(k).isAfter(upToDate)) {
            LocalDate dueDate = start.plusMonths(k);
            String forMonth = start.plusMonths(k - 1).format(MONTH_FMT);

            if (!rentPaymentRepository.existsByLeaseIdAndForMonth(lease.getId(), forMonth)) {
                RentPayment rent = new RentPayment();
                rent.setLease(lease);
                rent.setForMonth(forMonth);
                rent.setAmount(lease.getMonthlyRent());
                rent.setDueDate(dueDate);
                rent.setFine(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                rent.setStatus(RentStatus.PENDING);
                rentPaymentRepository.save(rent);
                generated++;
            }
            k++;
        }
        return generated;
    }

    @Transactional
    public RentGenerationResult runDailyRentJob() {
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);
        return generateRentForLeases(activeLeases);
    }

    @Transactional
    public RentGenerationResult generateRentForOwner(long ownerId) {
        List<Lease> activeLeases = leaseRepository.findByProperty_OwnerIdAndStatus(
                ownerId, LeaseStatus.ACTIVE);
        return generateRentForLeases(activeLeases);
    }

    private RentGenerationResult generateRentForLeases(List<Lease> activeLeases) {
        int generated = 0;
        for (Lease lease : activeLeases) {
            generated += generateRentForLease(lease);
            applyFinesForLease(lease);
        }
        return new RentGenerationResult(activeLeases.size(), generated);
    }

    public record RentGenerationResult(int leasesChecked, int rentRecordsCreated) {}

    @Transactional
    public void applyFinesForLease(Lease lease) {
        LocalDate today = LocalDate.now(clock);
        List<RentPayment> rents = rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(lease.getId());

        for (RentPayment rent : rents) {
            boolean unpaid = RentStatus.PENDING == rent.getStatus()
                    || RentStatus.LATE == rent.getStatus();
            if (!unpaid) {
                continue;
            }

            if (isOverdue(rent.getDueDate(), today, lease)) {
                rent.setFine(computeFine(rent.getDueDate(), today, lease));
                rent.setStatus(RentStatus.LATE);
                rentPaymentRepository.save(rent);
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void scheduledDailyRentJob() {
        runDailyRentJob();
    }

    @Transactional
    public RentResponse payOnline(Long rentId, Long tenantId) {
        RentPayment rent = rentPaymentRepository.findByIdAndLease_TenantId(rentId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found : " + rentId));

        if (!PAYABLE_STATUSES.contains(rent.getStatus())) {
            throw new IllegalStateException("Rent is not payable (current status: " + rent.getStatus() + ")");
        }

        LocalDate today = LocalDate.now(clock);
        BigDecimal finalFine = computeFine(rent.getDueDate(), today, rent.getLease());
        String txnId = UUID.randomUUID().toString();

        int updated = rentPaymentRepository.markPaidOnlineIfPayable(
                rentId, tenantId, RentStatus.PAID, PaymentMethod.ONLINE,
                txnId, today, finalFine, PAYABLE_STATUSES);
        if (updated == 0) {
            throw new IllegalStateException("Rent is already paid or awaiting approval");
        }

        RentPayment saved = rentPaymentRepository.findByIdAndLease_TenantId(rentId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found : " + rentId));
        return toResponse(saved);
    }

    private RentResponse toResponse(RentPayment rent) {
        RentResponse r = new RentResponse();
        r.setId(rent.getId());
        r.setLeaseId(rent.getLease().getId());
        r.setForMonth(rent.getForMonth());
        r.setAmount(rent.getAmount());
        r.setFine(rent.getFine());
        r.setDueDate(rent.getDueDate());
        r.setPaidDate(rent.getPaidDate());
        r.setMethod(rent.getMethod() != null ? rent.getMethod().name() : null);
        r.setTransactionId(rent.getTransactionId());
        r.setStatus(rent.getStatus() != null ? rent.getStatus().name() : null);
        return r;
    }

    @Transactional
    public RentResponse payCash(Long rentID, Long tenantId) {
        RentPayment rent = rentPaymentRepository.findByIdAndLease_TenantId(rentID, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found"));

        if (!PAYABLE_STATUSES.contains(rent.getStatus())) {
            throw new IllegalStateException("Rent is not payable (status: " + rent.getStatus() + ")");
        }

        LocalDate today = LocalDate.now(clock);
        BigDecimal frozenFine = computeFine(rent.getDueDate(), today, rent.getLease());

        int updated = rentPaymentRepository.markCashClaimIfPayable(
                rentID, tenantId, RentStatus.AWAITING_APPROVAL, PaymentMethod.CASH,
                frozenFine, PAYABLE_STATUSES);
        if (updated == 0) {
            throw new IllegalStateException("Rent is already paid or awaiting approval");
        }

        RentPayment saved = rentPaymentRepository.findByIdAndLease_TenantId(rentID, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found"));
        return toResponse(saved);
    }

    @Transactional
    public RentResponse approveCash(Long rentId, Long ownerId) {
        RentPayment rent = rentPaymentRepository.findByIdAndLease_Property_OwnerId(rentId, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found: " + rentId));

        if (RentStatus.AWAITING_APPROVAL != rent.getStatus()) {
            throw new IllegalStateException("Rent is not awaiting approval (status: " + rent.getStatus() + ")");
        }

        LocalDate today = LocalDate.now(clock);
        int updated = rentPaymentRepository.approveCashIfAwaiting(
                rentId, ownerId, RentStatus.PAID, today, RentStatus.AWAITING_APPROVAL);
        if (updated == 0) {
            throw new IllegalStateException("Rent is no longer awaiting approval");
        }

        RentPayment saved = rentPaymentRepository.findByIdAndLease_Property_OwnerId(rentId, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found: " + rentId));
        return toResponse(saved);
    }

    @Transactional
    public RentResponse rejectCash(Long rentId, Long ownerId) {
        RentPayment rent = rentPaymentRepository.findByIdAndLease_Property_OwnerId(rentId, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found: " + rentId));

        if (RentStatus.AWAITING_APPROVAL != rent.getStatus()) {
            throw new IllegalStateException("Rent is not awaiting approval (status: " + rent.getStatus() + ")");
        }

        LocalDate today = LocalDate.now(clock);
        Lease lease = rent.getLease();
        boolean overdue = isOverdue(rent.getDueDate(), today, lease);
        RentStatus resumedStatus = overdue ? RentStatus.LATE : RentStatus.PENDING;
        BigDecimal resumedFine = overdue
                ? computeFine(rent.getDueDate(), today, lease)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        int updated = rentPaymentRepository.rejectCashIfAwaiting(
                rentId, ownerId, resumedStatus, resumedFine, RentStatus.AWAITING_APPROVAL);
        if (updated == 0) {
            throw new IllegalStateException("Rent is no longer awaiting approval");
        }

        RentPayment saved = rentPaymentRepository.findByIdAndLease_Property_OwnerId(rentId, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found: " + rentId));
        return toResponse(saved);
    }

    private BigDecimal computeFine(LocalDate dueDate, LocalDate asOf, Lease lease) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, asOf);
        long chargeableDays = daysLate - lease.getFineGraceDays();
        if (chargeableDays <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return lease.getFinePerDay()
                .multiply(BigDecimal.valueOf(chargeableDays))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isOverdue(LocalDate dueDate, LocalDate asOf, Lease lease) {
        return ChronoUnit.DAYS.between(dueDate, asOf) > lease.getFineGraceDays();
    }

    public List<RentResponse> getPendingApprovals(Long ownerId) {
        return rentPaymentRepository
                .findByLease_Property_OwnerIdAndStatus(ownerId, RentStatus.AWAITING_APPROVAL)
                .stream().map(this::toResponse).toList();
    }

    public DuesResponse getDues(long tenantId) {
        List<RentStatus> unpaidStatus = List.of(
                RentStatus.PENDING, RentStatus.LATE, RentStatus.AWAITING_APPROVAL);

        List<RentPayment> unpaid = rentPaymentRepository.findByLease_TenantIdAndStatusIn(tenantId, unpaidStatus);
        BigDecimal total = BigDecimal.ZERO;
        LocalDate nextDue = null;
        for (RentPayment rent : unpaid) {
            total = total.add(rent.getAmount()).add(rent.getFine());
            if (nextDue == null || rent.getDueDate().isBefore(nextDue)) {
                nextDue = rent.getDueDate(); // earliest unpaid due date
            }
        }
        DuesResponse dues = new DuesResponse();
        dues.setTotalDue(total);
        dues.setNextDueDate(nextDue);
        dues.setBreakdown(unpaid.stream().map(this::toResponse).toList());
        return dues;
    }

    @Transactional(readOnly = true)
    public TenantRentsResponse getMyRents(Long tenantId) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + tenantId));
        requireRole(tenant, UserRole.TENANT);

        DuesResponse dues = getDues(tenantId);
        List<RentResponse> history = rentPaymentRepository
                .findByLease_TenantIdOrderByDueDateDesc(tenantId)
                .stream().map(this::toResponse).toList();

        TenantRentsResponse response = new TenantRentsResponse();
        response.setTotalDue(dues.getTotalDue());
        response.setNextDueDate(dues.getNextDueDate());
        response.setBreakdown(dues.getBreakdown());
        response.setHistory(history);
        return response;
    }

    @Transactional(readOnly = true)
    public List<RentResponse> getLeaseHistory(Long leaseId, Long ownerId, Long tenantId) {
        if ((ownerId == null) == (tenantId == null)) {
            throw new IllegalArgumentException("Provide exactly one of ownerId or tenantId");
        }

        if (ownerId != null) {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + ownerId));
            requireRole(owner, UserRole.OWNER);
            leaseRepository.findByIdAndProperty_OwnerId(leaseId, ownerId)
                    .orElseThrow(() -> new NoSuchElementException("Lease not found: " + leaseId));
        } else {
            User tenant = userRepository.findById(tenantId)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + tenantId));
            requireRole(tenant, UserRole.TENANT);
            leaseRepository.findByIdAndTenantId(leaseId, tenantId)
                    .orElseThrow(() -> new NoSuchElementException("Lease not found: " + leaseId));
        }

        return rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(leaseId)
                .stream().map(this::toResponse).toList();
    }

    private void requireRole(User user, UserRole requiredRole) {
        if (requiredRole != user.getRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action is not allowed for this user role");
        }
    }

}
