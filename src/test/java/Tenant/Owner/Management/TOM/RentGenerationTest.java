package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.RentPaymentRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.RentService;

@ExtendWith(MockitoExtension.class)
class RentGenerationTest {
    @Mock
    private RentPaymentRepository rentPaymentRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RentService rentService;

    @Test
    void generatesRentWhenMonthlyDueDateHasArrived() {
        Lease lease = lease(LocalDate.now().minusMonths(1));
        when(rentPaymentRepository.existsByLeaseIdAndForMonth(12L,
                lease.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))))
                .thenReturn(false);

        int created = rentService.generateRentForLease(lease);

        assertEquals(1, created);
        verify(rentPaymentRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotGenerateRentBeforeFirstMonthlyDueDate() {
        Lease lease = lease(LocalDate.now());

        int created = rentService.generateRentForLease(lease);

        assertEquals(0, created);
        verify(rentPaymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void generationResultDistinguishesCheckedLeasesFromCreatedRentRecords() {
        Lease lease = lease(LocalDate.now());
        when(leaseRepository.findByStatus(Constants.LEASE_ACTIVE)).thenReturn(List.of(lease));
        when(rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(12L)).thenReturn(List.of());

        RentService.RentGenerationResult result = rentService.runDailyRentJob();

        assertEquals(1, result.leasesChecked());
        assertEquals(0, result.rentRecordsCreated());
    }

    @Test
    void ownerGenerationChecksOnlyThatOwnersActiveLeases() {
        long ownerId = 24L;
        Lease ownerLease = lease(LocalDate.now());
        when(leaseRepository.findByProperty_OwnerIdAndStatus(ownerId, Constants.LEASE_ACTIVE))
                .thenReturn(List.of(ownerLease));
        when(rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(12L)).thenReturn(List.of());

        RentService.RentGenerationResult result = rentService.generateRentForOwner(ownerId);

        assertEquals(1, result.leasesChecked());
        assertEquals(0, result.rentRecordsCreated());
        verify(leaseRepository).findByProperty_OwnerIdAndStatus(ownerId, Constants.LEASE_ACTIVE);
        verify(leaseRepository, never()).findByStatus(Constants.LEASE_ACTIVE);
    }

    private Lease lease(LocalDate startDate) {
        Lease lease = new Lease();
        lease.setId(12L);
        lease.setStartDate(startDate);
        lease.setMonthlyRent(new BigDecimal("18000.00"));
        lease.setFineGraceDays(5);
        lease.setFinePerDay(BigDecimal.ZERO);
        return lease;
    }
}
