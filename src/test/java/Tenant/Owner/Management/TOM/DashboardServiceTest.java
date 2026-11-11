package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.LeaseStatus;
import Tenant.Owner.Management.TOM.Constants.MaintenanceStatus;
import Tenant.Owner.Management.TOM.Constants.RentStatus;
import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceRequestRepository;
import Tenant.Owner.Management.TOM.Repository.RentPaymentRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.DashboardService;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private RentPaymentRepository rentPaymentRepository;
    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void ownerDashboardReturnsScopedCountsAndOutstandingRent() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(4L, UserRole.OWNER)));
        when(leaseRepository.countDistinctTenantsByOwnerIdAndStatus(4L, LeaseStatus.ACTIVE)).thenReturn(3L);
        when(rentPaymentRepository.sumOutstandingByOwnerIdAndStatusIn(4L,
                List.of(RentStatus.PENDING, RentStatus.LATE, RentStatus.AWAITING_APPROVAL)))
                .thenReturn(new BigDecimal("2750.00"));
        when(maintenanceRequestRepository.countByProperty_OwnerIdAndStatusIn(4L,
                List.of(MaintenanceStatus.OPEN, MaintenanceStatus.IN_PROGRESS))).thenReturn(2L);

        var dashboard = dashboardService.getOwnerDashboard(4L);

        assertEquals(3L, dashboard.getTenantCount());
        assertEquals(new BigDecimal("2750.00"), dashboard.getPendingRent());
        assertEquals(2L, dashboard.getOpenMaintenanceCount());
    }

    @Test
    void emptyRentAggregateReturnsZero() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(4L, UserRole.OWNER)));
        when(leaseRepository.countDistinctTenantsByOwnerIdAndStatus(4L, LeaseStatus.ACTIVE)).thenReturn(0L);
        when(rentPaymentRepository.sumOutstandingByOwnerIdAndStatusIn(4L,
                List.of(RentStatus.PENDING, RentStatus.LATE, RentStatus.AWAITING_APPROVAL)))
                .thenReturn(null);
        when(maintenanceRequestRepository.countByProperty_OwnerIdAndStatusIn(4L,
                List.of(MaintenanceStatus.OPEN, MaintenanceStatus.IN_PROGRESS))).thenReturn(0L);

        var dashboard = dashboardService.getOwnerDashboard(4L);

        assertEquals(BigDecimal.ZERO, dashboard.getPendingRent());
    }

    @Test
    void tenantCannotReadOwnerDashboard() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, UserRole.TENANT)));

        assertThrows(ResponseStatusException.class, () -> dashboardService.getOwnerDashboard(7L));
        verify(leaseRepository, never()).countDistinctTenantsByOwnerIdAndStatus(7L, LeaseStatus.ACTIVE);
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}