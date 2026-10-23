package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class RentServiceHistoryTest {
    @Mock
    private RentPaymentRepository rentPaymentRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RentService rentService;

    @Test
    void tenantCanReadOwnLeasePaymentHistory() {
        User tenant = user(7L, Constants.ROLE_TENANT);
        RentPayment payment = payment(12L, 5L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(leaseRepository.findByIdAndTenantId(5L, 7L)).thenReturn(Optional.of(new Lease()));
        when(rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(5L)).thenReturn(List.of(payment));

        var history = rentService.getLeaseHistory(5L, null, 7L);

        assertEquals(1, history.size());
        assertEquals(12L, history.get(0).getId());
        assertEquals(5L, history.get(0).getLeaseId());
        verify(rentPaymentRepository).findByLeaseIdOrderByDueDateDesc(5L);
    }

    @Test
    void ownerCanReadHistoryForOwnedLease() {
        User owner = user(2L, Constants.ROLE_OWNER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(leaseRepository.findByIdAndProperty_OwnerId(5L, 2L)).thenReturn(Optional.of(new Lease()));
        when(rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(5L)).thenReturn(List.of());

        var history = rentService.getLeaseHistory(5L, 2L, null);

        assertEquals(0, history.size());
        verify(rentPaymentRepository).findByLeaseIdOrderByDueDateDesc(5L);
    }

    @Test
    void unrelatedTenantCannotReadLeaseHistory() {
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, Constants.ROLE_TENANT)));
        when(leaseRepository.findByIdAndTenantId(5L, 8L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> rentService.getLeaseHistory(5L, null, 8L));
        verify(rentPaymentRepository, never()).findByLeaseIdOrderByDueDateDesc(5L);
    }

    @Test
    void historyRequiresExactlyOneIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> rentService.getLeaseHistory(5L, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> rentService.getLeaseHistory(5L, 2L, 7L));
    }

    @Test
    void wrongRoleCannotReadLeaseHistory() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, Constants.ROLE_OWNER)));

        assertThrows(ResponseStatusException.class,
                () -> rentService.getLeaseHistory(5L, null, 7L));
        verify(leaseRepository, never()).findByIdAndTenantId(5L, 7L);
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private RentPayment payment(Long id, Long leaseId) {
        Lease lease = new Lease();
        lease.setId(leaseId);
        RentPayment payment = new RentPayment();
        payment.setId(id);
        payment.setLease(lease);
        payment.setForMonth("2026-09");
        payment.setAmount(BigDecimal.valueOf(1250));
        payment.setFine(BigDecimal.ZERO);
        payment.setDueDate(LocalDate.of(2026, 10, 15));
        payment.setStatus(Constants.RENT_PAID);
        return payment;
    }
}