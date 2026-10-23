package Tenant.Owner.Management.TOM.Service;

import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.RentPayment;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import Tenant.Owner.Management.TOM.Constants.Constants;
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

    private final RentPaymentRepository rentPaymentRepository;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;

    public static final String RENT_PENDING = Constants.RENT_PENDING;
    public static final String RENT_LATE = Constants.RENT_LATE;
    public static final String RENT_AWAITING_APPROVAL = Constants.RENT_AWAITING_APPROVAL;
    public static final String RENT_PAID = Constants.RENT_PAID;
    public static final String METHOD_ONLINE = Constants.METHOD_ONLINE;
    public static final String METHOD_CASH = Constants.METHOD_CASH;

    public RentService(RentPaymentRepository rentPaymentRepository, LeaseRepository leaseRepository,
            UserRepository userRepository) {
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public int generateRentForLease(Lease lease) {
        LocalDate start = lease.getStartDate();
        LocalDate today = LocalDate.now();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");

        int k = 1;
        int generated = 0;
        while (!start.plusMonths(k).isAfter(today)) {
            LocalDate dueDate = start.plusMonths(k);
            String forMonth = start.plusMonths(k - 1).format(monthFmt);

            if (!rentPaymentRepository.existsByLeaseIdAndForMonth(lease.getId(), forMonth)) {
                RentPayment rent = new RentPayment();
                rent.setLease(lease);
                rent.setForMonth(forMonth);
                rent.setAmount(lease.getMonthlyRent());
                rent.setDueDate(dueDate);
                rent.setFine(java.math.BigDecimal.ZERO);
                rent.setStatus(Constants.RENT_PENDING);
                rentPaymentRepository.save(rent);
                generated++;
            }
            k++;
        }
        return generated;
    }

    @Transactional
    public RentGenerationResult runDailyRentJob() {
        List<Lease> activeLeases = leaseRepository.findByStatus(Constants.LEASE_ACTIVE);
        return generateRentForLeases(activeLeases);
    }

    @Transactional
    public RentGenerationResult generateRentForOwner(long ownerId) {
        List<Lease> activeLeases = leaseRepository.findByProperty_OwnerIdAndStatus(
                ownerId, Constants.LEASE_ACTIVE);
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
        LocalDate today = LocalDate.now();
        List<RentPayment> rents = rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(lease.getId());

        for (RentPayment rent : rents) {
            boolean unpaid = Constants.RENT_PENDING.equals(rent.getStatus())
                    || Constants.RENT_LATE.equals(rent.getStatus());
            if (!unpaid)
                continue;

            long daysLate = java.time.temporal.ChronoUnit.DAYS.between(rent.getDueDate(), today);
            long chargeableDays = daysLate - lease.getFineGraceDays();

            if (chargeableDays > 0) {
                java.math.BigDecimal fine = lease.getFinePerDay()
                        .multiply(java.math.BigDecimal.valueOf(chargeableDays));
                rent.setFine(fine);
                rent.setStatus(Constants.RENT_LATE);
                rentPaymentRepository.save(rent);
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledDailyRentJob() {
        runDailyRentJob();
    }

    @Transactional
    public RentResponse payOnline(Long rentId, Long tenantId) {
        RentPayment rent = rentPaymentRepository.findByIdAndLease_TenantId(rentId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found : " + rentId));

        boolean payable = Constants.RENT_PENDING.equals(rent.getStatus())
                || Constants.RENT_LATE.equals(rent.getStatus());

        if (!payable) {
            throw new IllegalStateException("Rent is not payable (current status: " + rent.getStatus() + ")");
        }

        rent.setStatus((Constants.RENT_PAID));
        rent.setMethod(Constants.METHOD_ONLINE);
        rent.setTransactionId(UUID.randomUUID().toString());
        rent.setPaidDate(LocalDate.now());

        RentPayment saved = rentPaymentRepository.save(rent);

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
        r.setMethod(rent.getMethod());
        r.setTransactionId(rent.getTransactionId());
        r.setStatus(rent.getStatus());
        return r;
    }

    @Transactional
    public RentResponse payCash(Long rentID, Long tenantId) {
        RentPayment rent = rentPaymentRepository.findByIdAndLease_TenantId(rentID, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found"));

        boolean payable = Constants.RENT_PENDING.equals(rent.getStatus())
                || Constants.RENT_LATE.equals(rent.getStatus());
        if (!payable) {
            throw new IllegalStateException("Rent is not payable (status: " + rent.getStatus() + ")");
        }
        rent.setStatus(Constants.RENT_AWAITING_APPROVAL);
        rent.setMethod(Constants.METHOD_CASH);
        return toResponse(rentPaymentRepository.save(rent));

    }

    @Transactional
    public RentResponse approveCash(Long rentId, Long ownerId) {
        RentPayment rent = rentPaymentRepository.findByIdAndLease_Property_OwnerId(rentId, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found: " + rentId));

        if (!Constants.RENT_AWAITING_APPROVAL.equals(rent.getStatus())) {
            throw new IllegalStateException("Rent is not awaiting approval (status: " + rent.getStatus() + ")");
        }

        rent.setStatus(RENT_PAID);
        rent.setPaidDate(LocalDate.now());

        return toResponse(rentPaymentRepository.save(rent));
    }

    @Transactional
    public RentResponse rejectCash(Long rentId, Long ownerId) {
        RentPayment rent = rentPaymentRepository.findByIdAndLease_Property_OwnerId(rentId, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Rent not found: " + rentId));

        if (!Constants.RENT_AWAITING_APPROVAL.equals(rent.getStatus())) {
            throw new IllegalStateException("Rent is not awaiting approval (status: " + rent.getStatus() + ")");
        }
        rent.setStatus(Constants.RENT_PENDING);
        rent.setMethod(null);
        return toResponse(rentPaymentRepository.save(rent));
    }

    public List<RentResponse> getPendingApprovals(Long ownerId) {
        return rentPaymentRepository
                .findByLease_Property_OwnerIdAndStatus(ownerId, Constants.RENT_AWAITING_APPROVAL)
                .stream().map(this::toResponse).toList();
    }

    public DuesResponse getDues(long tenantId) {
        List<String> unpaidStatus = List.of(RENT_PENDING, RENT_LATE, RENT_AWAITING_APPROVAL);

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
        requireRole(tenant, Constants.ROLE_TENANT);

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
            requireRole(owner, Constants.ROLE_OWNER);
            leaseRepository.findByIdAndProperty_OwnerId(leaseId, ownerId)
                    .orElseThrow(() -> new NoSuchElementException("Lease not found: " + leaseId));
        } else {
            User tenant = userRepository.findById(tenantId)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + tenantId));
            requireRole(tenant, Constants.ROLE_TENANT);
            leaseRepository.findByIdAndTenantId(leaseId, tenantId)
                    .orElseThrow(() -> new NoSuchElementException("Lease not found: " + leaseId));
        }

        return rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(leaseId)
                .stream().map(this::toResponse).toList();
    }

    private void requireRole(User user, String requiredRole) {
        if (!requiredRole.equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action is not allowed for this user role");
        }
    }

}
