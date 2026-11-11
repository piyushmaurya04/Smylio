package Tenant.Owner.Management.TOM.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import Tenant.Owner.Management.TOM.Constants.LeaseStatus;
import Tenant.Owner.Management.TOM.Constants.PropertyStatus;
import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.DTO.LeaseRequest;
import Tenant.Owner.Management.TOM.DTO.LeaseResponse;
import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaseService {
    private final LeaseRepository leaseRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final RentService rentService;
    private final Clock clock;

    public LeaseService(LeaseRepository leaseRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            RentService rentService,
            Clock clock) {
        this.leaseRepository = leaseRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.rentService = rentService;
        this.clock = clock;
    }

    @Transactional
    public LeaseResponse create(LeaseRequest request, Long ownerId) {
        if (request == null || request.getPropertyId() == null) {
            throw new IllegalArgumentException("propertyId is required");
        }
        if (request.getTenantCode() == null || request.getTenantCode().isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank");
        }
        validateLeaseTerms(request.getMonthlyRent(), request.getFineGraceDays(), request.getFinePerDay());
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        Property property = propertyRepository.findByIdAndOwnerIdForUpdate(request.getPropertyId(), ownerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Property Not found " + request.getPropertyId()));

        if (PropertyStatus.OCCUPIED == property.getStatus()
                || leaseRepository.existsByPropertyIdAndStatus(property.getId(), LeaseStatus.ACTIVE)) {
            throw new IllegalStateException("Property already occupied");
        }

        String normalizedCode = request.getTenantCode().trim().toUpperCase(Locale.ROOT);
        User tenant = userRepository.findByTenantCode(normalizedCode)
                .filter(user -> UserRole.TENANT == user.getRole())
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Tenant not Found, Please check the Tenant Code and try again " + normalizedCode));

        Lease lease = new Lease();
        lease.setProperty(property);
        lease.setTenant(tenant);
        lease.setStartDate(request.getStartDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setFineGraceDays(request.getFineGraceDays());
        lease.setFinePerDay(request.getFinePerDay());
        lease.setStatus(LeaseStatus.ACTIVE);
        Lease savedLease = leaseRepository.save(lease);

        property.setStatus(PropertyStatus.OCCUPIED);
        propertyRepository.save(property);

        return toResponse(savedLease);

    }

    @Transactional
    public LeaseResponse endLease(Long id, long ownerId) {
        Lease lease = leaseRepository.findByIdAndProperty_OwnerId(id, ownerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Lease Not found"));

        if (LeaseStatus.ACTIVE != lease.getStatus()) {
            throw new IllegalStateException("lease is already ended");
        }
        LocalDate endDate = LocalDate.now(clock);
        lease.setStatus(LeaseStatus.ENDED);
        lease.setEndDate(endDate);
        Lease saved = leaseRepository.save(lease);

        rentService.generateFinalRentOnLeaseEnd(saved, endDate);

        Property property = saved.getProperty();
        property.setStatus(PropertyStatus.AVAILABLE);

        propertyRepository.save(property);

        return toResponse(saved);

    }

    @Transactional
    public LeaseResponse leaseUpdate(Long id, LeaseRequest request, Long ownerId) {
        if (request == null) {
            throw new IllegalArgumentException("Lease update details are required");
        }
        validateLeaseTerms(request.getMonthlyRent(), request.getFineGraceDays(), request.getFinePerDay());

        Lease lease = leaseRepository.findByIdAndProperty_OwnerId(id, ownerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Lease not found"));
        if (LeaseStatus.ACTIVE != lease.getStatus()) {
            throw new IllegalStateException("Only active leases can be updated");
        }

        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setFineGraceDays(request.getFineGraceDays());
        lease.setFinePerDay(request.getFinePerDay());

        Lease saved = leaseRepository.save(lease);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaseResponse> getMyLeases(Long ownerId) {
        List<Lease> leases = leaseRepository.findOwnerLeasesWithDetails(ownerId);
        if (leases.isEmpty()) {
            return List.of();
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Property owner not found"));
        return leases.stream().map(lease -> toResponse(lease, owner)).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaseResponse> getMyActiveLeases(Long tenantId) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + tenantId));
        if (UserRole.TENANT != tenant.getRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action is not allowed for this user role");
        }
        List<Lease> leases = leaseRepository.findTenantLeasesWithDetails(tenantId, LeaseStatus.ACTIVE);
        if (leases.isEmpty()) {
            return List.of();
        }
        List<Long> ownerIds = leases.stream()
                .map(lease -> lease.getProperty().getOwnerId())
                .distinct()
                .toList();
        Map<Long, User> ownersById = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return leases.stream()
                .map(lease -> {
                    User owner = ownersById.get(lease.getProperty().getOwnerId());
                    if (owner == null) {
                        throw new java.util.NoSuchElementException("Property owner not found");
                    }
                    return toResponse(lease, owner);
                })
                .toList();
    }

    public LeaseResponse toResponse(Lease lease) {
        User owner = userRepository.findById(lease.getProperty().getOwnerId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Property owner not found"));
        return toResponse(lease, owner);
    }

    private LeaseResponse toResponse(Lease lease, User owner) {
        LeaseResponse response = new LeaseResponse();
        response.setId(lease.getId());
        response.setPropertyId(lease.getProperty().getId());
        response.setPropertyTitle(lease.getProperty().getTitle());
        response.setAmenities(lease.getProperty().getAmenities().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER).toList());
        response.setTenantId(lease.getTenant().getId());
        response.setTenantName(lease.getTenant().getName());
        response.setTenantCode(lease.getTenant().getTenantCode());
        response.setOwnerName(owner.getName());
        response.setOwnerEmail(owner.getEmail());
        response.setOwnerPhone(owner.getPhone());
        response.setStartDate(lease.getStartDate());
        response.setEndDate(lease.getEndDate());
        response.setMonthlyRent(lease.getMonthlyRent());
        response.setFineGraceDays(lease.getFineGraceDays());
        response.setFinePerDay(lease.getFinePerDay());
        response.setStatus(lease.getStatus() != null ? lease.getStatus().name() : null);

        return response;
    }

    private void validateLeaseTerms(BigDecimal monthlyRent, Integer fineGraceDays, BigDecimal finePerDay) {
        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("monthlyRent must be greater than 0");
        }
        if (fineGraceDays == null || fineGraceDays < 0) {
            throw new IllegalArgumentException("fineGraceDays must be 0 or greater");
        }
        if (finePerDay == null || finePerDay.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("finePerDay must be 0 or greater");
        }
    }

}
