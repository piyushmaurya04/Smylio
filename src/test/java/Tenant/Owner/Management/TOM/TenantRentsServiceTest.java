package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.RentPayment;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.RentPaymentRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.RentService;

@ExtendWith(MockitoExtension.class)
class TenantRentsServiceTest {
    @Mock
    private RentPaymentRepository rentPaymentRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RentService rentService;

    @Test
    void tenantSummaryIncludesUnpaidDuesAndCompleteHistoryAcrossLeases() {
        User tenant = user(7L, Constants.ROLE_TENANT);
        RentPayment pending = payment(21L, 5L, "2026-10", "PENDING", "500.00", "0.00");
        RentPayment paid = payment(20L, 4L, "2026-09", "PAID", "450.00", "0.00");
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(rentPaymentRepository.findByLease_TenantIdAndStatusIn(7L,
                List.of(Constants.RENT_PENDING, Constants.RENT_LATE, Constants.RENT_AWAITING_APPROVAL)))
                .thenReturn(List.of(pending));
        when(rentPaymentRepository.findByLease_TenantIdOrderByDueDateDesc(7L))
                .thenReturn(List.of(pending, paid));

        var response = rentService.getMyRents(7L);

        assertEquals(new BigDecimal("500.00"), response.getTotalDue());
        assertEquals(LocalDate.of(2026, 11, 15), response.getNextDueDate());
        assertEquals(1, response.getBreakdown().size());
        assertEquals(2, response.getHistory().size());
        assertEquals(Constants.RENT_PAID, response.getHistory().get(1).getStatus());
    }

    @Test
    void ownerCannotUseTenantRentSummary() {
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, Constants.ROLE_OWNER)));

        assertThrows(ResponseStatusException.class, () -> rentService.getMyRents(8L));
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private RentPayment payment(Long id, Long leaseId, String month, String status,
            String amount, String fine) {
        Lease lease = new Lease();
        lease.setId(leaseId);
        lease.setTenant(user(7L, Constants.ROLE_TENANT));
        RentPayment payment = new RentPayment();
        payment.setId(id);
        payment.setLease(lease);
        payment.setForMonth(month);
        payment.setAmount(new BigDecimal(amount));
        payment.setFine(new BigDecimal(fine));
        payment.setDueDate(LocalDate.of(2026, 11, 15));
        payment.setStatus(status);
        return payment;
    }
}