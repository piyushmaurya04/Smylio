package Tenant.Owner.Management.TOM.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.DTO.LeaseResponse;
import Tenant.Owner.Management.TOM.DTO.TenantDetailsResponse;
import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceRequestRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@Service
public class TenantDetailsService {
    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;

    public TenantDetailsService(
            UserRepository userRepository,
            LeaseRepository leaseRepository,
            MaintenanceRequestRepository maintenanceRequestRepository) {
        this.userRepository = userRepository;
        this.leaseRepository = leaseRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
    }

    @Transactional(readOnly = true)
    public TenantDetailsResponse getTenantDetails(Long tenantId, Long ownerId) {
        List<Lease> leases = leaseRepository.findOwnerTenantLeasesWithDetails(tenantId, ownerId);
        if (leases.isEmpty()) {
            throw new NoSuchElementException("Tenant not found for this owner");
        }

        User tenant = userRepository.findById(tenantId)
                .filter(user -> Constants.ROLE_TENANT.equals(user.getRole()))
                .orElseThrow(() -> new NoSuchElementException("Tenant not found for this owner"));

        TenantDetailsResponse response = new TenantDetailsResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setEmail(tenant.getEmail());
        response.setPhone(tenant.getPhone());
        response.setTenantCode(tenant.getTenantCode());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setMaintenanceRequestCount(
                maintenanceRequestRepository.countByTenantIdAndProperty_OwnerId(tenantId, ownerId));
        Map<Long, Long> maintenanceRequestsByProperty = new LinkedHashMap<>();
        leases.stream().map(lease -> lease.getProperty().getId()).distinct().forEach(propertyId ->
                maintenanceRequestsByProperty.put(propertyId,
                        maintenanceRequestRepository.countByTenantIdAndPropertyIdAndProperty_OwnerId(
                                tenantId, propertyId, ownerId)));
        response.setMaintenanceRequestsByProperty(maintenanceRequestsByProperty);
        response.setLeases(leases.stream().map(this::toLeaseResponse).toList());
        return response;
    }

    private LeaseResponse toLeaseResponse(Lease lease) {
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
