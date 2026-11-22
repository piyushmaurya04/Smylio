package Tenant.Owner.Management.TOM.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.LeaseStatus;
import Tenant.Owner.Management.TOM.Constants.MaintenanceStatus;
import Tenant.Owner.Management.TOM.Constants.RentStatus;
import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.DTO.OwnerDashboardResponse;
import Tenant.Owner.Management.TOM.DTO.WorkspaceBadgesResponse;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceRequestRepository;
import Tenant.Owner.Management.TOM.Repository.NoticeRepository;
import Tenant.Owner.Management.TOM.Repository.RentPaymentRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@Service
public class DashboardService {
    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final NoticeRepository noticeRepository;

    public DashboardService(UserRepository userRepository, LeaseRepository leaseRepository,
            RentPaymentRepository rentPaymentRepository,
            MaintenanceRequestRepository maintenanceRequestRepository,
            NoticeRepository noticeRepository) {
        this.userRepository = userRepository;
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.noticeRepository = noticeRepository;
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getOwnerDashboard(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + ownerId));
        if (!owner.hasRole(UserRole.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action is not allowed for this user role");
        }

        OwnerDashboardResponse response = new OwnerDashboardResponse();
        response.setTenantCount(leaseRepository.countDistinctTenantsByOwnerIdAndStatus(
                ownerId, LeaseStatus.ACTIVE));
        response.setPendingRent(rentPaymentRepository.sumOutstandingByOwnerIdAndStatusIn(ownerId,
                List.of(RentStatus.PENDING, RentStatus.LATE, RentStatus.AWAITING_APPROVAL)));
        response.setOpenMaintenanceCount(maintenanceRequestRepository.countByProperty_OwnerIdAndStatusIn(
                ownerId, List.of(MaintenanceStatus.OPEN, MaintenanceStatus.IN_PROGRESS)));
        if (response.getPendingRent() == null) {
            response.setPendingRent(BigDecimal.ZERO);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public WorkspaceBadgesResponse getWorkspaceBadges(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        WorkspaceBadgesResponse response = new WorkspaceBadgesResponse();

        if (user.hasRole(UserRole.OWNER)) {
            int pendingApprovals = (int) rentPaymentRepository.countByLease_Property_OwnerIdAndStatus(userId, RentStatus.AWAITING_APPROVAL);
            int openTickets = (int) maintenanceRequestRepository.countByProperty_OwnerIdAndStatusIn(userId,
                    List.of(MaintenanceStatus.OPEN, MaintenanceStatus.IN_PROGRESS));
            response.setOwnerPendingApprovals(pendingApprovals);
            response.setOwnerOpenTickets(openTickets);
        }

        if (user.hasRole(UserRole.TENANT)) {
            int pendingRents = (int) rentPaymentRepository.countByLease_TenantIdAndStatusIn(userId,
                    List.of(RentStatus.PENDING, RentStatus.LATE, RentStatus.AWAITING_APPROVAL));
            int visibleNotices = noticeRepository.findVisibleToTenant(userId, LeaseStatus.ACTIVE).size();
            response.setTenantPendingRents(pendingRents);
            response.setTenantUnreadNotices(visibleNotices);
        }

        return response;
    }
}