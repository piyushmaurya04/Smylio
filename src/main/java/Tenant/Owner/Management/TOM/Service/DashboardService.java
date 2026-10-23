package Tenant.Owner.Management.TOM.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.DTO.OwnerDashboardResponse;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceRequestRepository;
import Tenant.Owner.Management.TOM.Repository.RentPaymentRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@Service
public class DashboardService {
    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;

    public DashboardService(UserRepository userRepository, LeaseRepository leaseRepository,
            RentPaymentRepository rentPaymentRepository,
            MaintenanceRequestRepository maintenanceRequestRepository) {
        this.userRepository = userRepository;
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getOwnerDashboard(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + ownerId));
        if (!Constants.ROLE_OWNER.equals(owner.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action is not allowed for this user role");
        }

        OwnerDashboardResponse response = new OwnerDashboardResponse();
        response.setTenantCount(leaseRepository.countDistinctTenantsByOwnerIdAndStatus(
                ownerId, Constants.LEASE_ACTIVE));
        response.setPendingRent(rentPaymentRepository.sumOutstandingByOwnerIdAndStatusIn(ownerId,
                List.of(Constants.RENT_PENDING, Constants.RENT_LATE, Constants.RENT_AWAITING_APPROVAL)));
        response.setOpenMaintenanceCount(maintenanceRequestRepository.countByProperty_OwnerIdAndStatusIn(
                ownerId, List.of(Constants.MAINTENANCE_OPEN, Constants.MAINTENANCE_IN_PROGRESS)));
        if (response.getPendingRent() == null) {
            response.setPendingRent(BigDecimal.ZERO);
        }
        return response;
    }
}