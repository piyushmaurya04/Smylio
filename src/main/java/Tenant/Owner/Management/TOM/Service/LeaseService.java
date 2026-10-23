package Tenant.Owner.Management.TOM.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import Tenant.Owner.Management.TOM.Constants.*;
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

    public static final String LEASE_ACTIVE = Constants.LEASE_ACTIVE;
    public static final String LEASE_ENDED = Constants.LEASE_ENDED;
    public static final String STATUS_AVAILABLE = Constants.STATUS_AVAILABLE;
    public static final String STATUS_OCCUPIED = Constants.STATUS_OCCUPIED;

    public LeaseService(LeaseRepository leaseRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository) {
        this.leaseRepository = leaseRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LeaseResponse create(LeaseRequest request, Long ownerId) {

        Property property = propertyRepository.findByIdAndOwnerId(request.getPropertyId(), ownerId).orElseThrow(
                () -> new java.util.NoSuchElementException("Proprty Not found " + request.getPropertyId()));

        if (leaseRepository.existsByPropertyIdAndStatus(property.getId(), LEASE_ACTIVE)) {
            throw new IllegalStateException("Property already occupied");

        }

        User tenant = userRepository.findByTenantCode(request.getTenantCode())
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Tenant not Found, Please check the Tenant Code and try again " + request.getTenantCode()));

        Lease lease = new Lease();
        lease.setProperty(property);
        lease.setTenant(tenant);
        lease.setStartDate(request.getStartDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setFineGraceDays(request.getFineGraceDays());
        lease.setFinePerDay(request.getFinePerDay());
        lease.setStatus(Constants.LEASE_ACTIVE);
        Lease savedLease = leaseRepository.save(lease);

        property.setStatus(STATUS_OCCUPIED);
        propertyRepository.save(property);

        return toResponse(savedLease);

    }

    @Transactional
    public LeaseResponse endLease(Long id, long ownerId) {
        Lease lease = leaseRepository.findByIdAndProperty_OwnerId(id, ownerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Lease Not found"));

        if (!LEASE_ACTIVE.equals(lease.getStatus())) {
            throw new IllegalStateException("lease is already ended");
        }
        lease.setStatus(LEASE_ENDED);
        lease.setEndDate(LocalDate.now());
        Lease saved = leaseRepository.save(lease);

        Property property = saved.getProperty();
        property.setStatus(STATUS_AVAILABLE);

        propertyRepository.save(property);

        return toResponse(lease);

    }

    @Transactional
    public LeaseResponse leaseUpdate(Long id, LeaseRequest request, Long ownerId) {
        Lease lease = leaseRepository.findByIdAndProperty_OwnerId(id, ownerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Lease not found"));

        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setFineGraceDays(request.getFineGraceDays());
        lease.setFinePerDay(request.getFinePerDay());

        Lease saved = leaseRepository.save(lease);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaseResponse> getMyLeases(Long ownerId) {

        return leaseRepository.findOwnerLeasesWithDetails(ownerId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaseResponse> getMyActiveLeases(Long tenantId) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + tenantId));
        if (!Constants.ROLE_TENANT.equals(tenant.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action is not allowed for this user role");
        }
        return leaseRepository.findTenantLeasesWithDetails(tenantId, LEASE_ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    public LeaseResponse toResponse(Lease lease) {
        LeaseResponse response = new LeaseResponse();
        response.setId(lease.getId());
        response.setPropertyId(lease.getProperty().getId());
        response.setPropertyTitle(lease.getProperty().getTitle());
        response.setAmenities(lease.getProperty().getAmenities().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER).toList());
        response.setTenantId(lease.getTenant().getId());
        response.setTenantName(lease.getTenant().getName());
        response.setTenantCode(lease.getTenant().getTenantCode());
        response.setStartDate(lease.getStartDate());
        response.setEndDate(lease.getEndDate());
        response.setMonthlyRent(lease.getMonthlyRent());
        response.setFineGraceDays(lease.getFineGraceDays());
        response.setFinePerDay(lease.getFinePerDay());
        response.setStatus(lease.getStatus());

        return response;
    }

}
