package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Tenant.Owner.Management.TOM.Constants.PaymentMethod;
import Tenant.Owner.Management.TOM.Constants.RentStatus;
import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Entity.RentPayment;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.RentPaymentRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.RentService;

@ExtendWith(MockitoExtension.class)
class RentBillingAndPaymentsTest {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Mock
    private RentPaymentRepository rentPaymentRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private UserRepository userRepository;

    @Test
    void clampsJanuary31StartToFeb28InNonLeapYearAndFeb29InLeapYear() {
        RentService nonLeapService = serviceAt(2026, 2, 28);
        Lease nonLeapLease = lease(LocalDate.of(2026, 1, 31));
        when(rentPaymentRepository.existsByLeaseIdAndForMonth(12L, "2026-01")).thenReturn(false);

        int createdNonLeap = nonLeapService.generateRentForLease(nonLeapLease);

        assertEquals(1, createdNonLeap);
        ArgumentCaptor<RentPayment> nonLeapCaptor = ArgumentCaptor.forClass(RentPayment.class);
        verify(rentPaymentRepository).save(nonLeapCaptor.capture());
        assertEquals(LocalDate.of(2026, 2, 28), nonLeapCaptor.getValue().getDueDate());

        RentService leapService = serviceAt(2024, 2, 29);
        Lease leapLease = lease(LocalDate.of(2024, 1, 31));
        when(rentPaymentRepository.existsByLeaseIdAndForMonth(12L, "2024-01")).thenReturn(false);

        int createdLeap = leapService.generateRentForLease(leapLease);

        assertEquals(1, createdLeap);
        ArgumentCaptor<RentPayment> leapCaptor = ArgumentCaptor.forClass(RentPayment.class);
        verify(rentPaymentRepository, times(2)).save(leapCaptor.capture());
        assertEquals(LocalDate.of(2024, 2, 29), leapCaptor.getValue().getDueDate());
    }

    @Test
    void istClockCrossesMidnightBeforeUtcDoes() {
        // 2026-02-28T19:00:00Z is 2026-03-01 00:30 IST
        Clock istClock = Clock.fixed(
                ZonedDateTime.of(2026, 3, 1, 0, 30, 0, 0, IST).toInstant(), IST);
        RentService service = new RentService(rentPaymentRepository, leaseRepository, userRepository, istClock);
        Lease lease = lease(LocalDate.of(2026, 2, 1));
        when(rentPaymentRepository.existsByLeaseIdAndForMonth(12L, "2026-02")).thenReturn(false);

        int created = service.generateRentForLease(lease);

        assertEquals(1, created);
    }

    @Test
    void payOnlineRecomputesAndFreezesFineAtomically() {
        // Due 2026-02-10, grace 5 days, today 2026-02-18 -> 8 days late - 5 grace = 3 chargeable days * 100 = 300.00
        RentService service = serviceAt(2026, 2, 18);
        Lease lease = lease(LocalDate.of(2026, 1, 10));
        RentPayment rent = payment(99L, lease, LocalDate.of(2026, 2, 10), RentStatus.PENDING);

        RentPayment updatedRent = payment(99L, lease, LocalDate.of(2026, 2, 10), RentStatus.PAID);
        updatedRent.setFine(new BigDecimal("300.00"));
        updatedRent.setMethod(PaymentMethod.ONLINE);
        updatedRent.setPaidDate(LocalDate.of(2026, 2, 18));

        when(rentPaymentRepository.findByIdAndLease_TenantId(99L, 7L))
                .thenReturn(Optional.of(rent))
                .thenReturn(Optional.of(updatedRent));
        when(rentPaymentRepository.markPaidOnlineIfPayable(
                eq(99L), eq(7L), eq(RentStatus.PAID), eq(PaymentMethod.ONLINE),
                anyString(), eq(LocalDate.of(2026, 2, 18)), eq(new BigDecimal("300.00")),
                eq(List.of(RentStatus.PENDING, RentStatus.LATE))))
                .thenReturn(1);

        var response = service.payOnline(99L, 7L);

        assertEquals(RentStatus.PAID.name(), response.getStatus());
        assertEquals(new BigDecimal("300.00"), response.getFine());
    }

    @Test
    void concurrentPayOnlineThrowsConflictWhenConditionalUpdateAffectsZeroRows() {
        RentService service = serviceAt(2026, 2, 18);
        Lease lease = lease(LocalDate.of(2026, 1, 10));
        RentPayment rent = payment(99L, lease, LocalDate.of(2026, 2, 10), RentStatus.PENDING);

        when(rentPaymentRepository.findByIdAndLease_TenantId(99L, 7L)).thenReturn(Optional.of(rent));
        when(rentPaymentRepository.markPaidOnlineIfPayable(
                eq(99L), eq(7L), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.payOnline(99L, 7L));
    }

    @Test
    void rejectCashRestoresLateStatusAndResumedFineWhenOverdue() {
        RentService service = serviceAt(2026, 2, 20);
        Lease lease = lease(LocalDate.of(2026, 1, 10)); // grace 5, fine 100/day -> 10 days late -> 5 * 100 = 500.00
        RentPayment awaiting = payment(99L, lease, LocalDate.of(2026, 2, 10), RentStatus.AWAITING_APPROVAL);
        RentPayment rejected = payment(99L, lease, LocalDate.of(2026, 2, 10), RentStatus.LATE);
        rejected.setFine(new BigDecimal("500.00"));

        when(rentPaymentRepository.findByIdAndLease_Property_OwnerId(99L, 4L))
                .thenReturn(Optional.of(awaiting))
                .thenReturn(Optional.of(rejected));
        when(rentPaymentRepository.rejectCashIfAwaiting(
                99L, 4L, RentStatus.LATE, new BigDecimal("500.00"), RentStatus.AWAITING_APPROVAL))
                .thenReturn(1);

        var response = service.rejectCash(99L, 4L);

        assertEquals(RentStatus.LATE.name(), response.getStatus());
        assertEquals(new BigDecimal("500.00"), response.getFine());
    }

    @Test
    void generateFinalRentOnLeaseEndProratesMidCycleBill() {
        // Lease starts 2026-02-01, ends 2026-02-11 (10 of 28 days in cycle Feb 1..Mar 1)
        // 14000.00 * 10 / 28 = 5000.00
        RentService service = serviceAt(2026, 2, 11);
        Lease lease = lease(LocalDate.of(2026, 2, 1));
        lease.setMonthlyRent(new BigDecimal("14000.00"));
        when(rentPaymentRepository.existsByLeaseIdAndForMonth(12L, "2026-02")).thenReturn(false);
        when(rentPaymentRepository.findByLeaseIdOrderByDueDateDesc(12L)).thenReturn(List.of());

        int created = service.generateFinalRentOnLeaseEnd(lease, LocalDate.of(2026, 2, 11));

        assertEquals(1, created);
        ArgumentCaptor<RentPayment> captor = ArgumentCaptor.forClass(RentPayment.class);
        verify(rentPaymentRepository).save(captor.capture());
        assertEquals("2026-02", captor.getValue().getForMonth());
        assertEquals(new BigDecimal("5000.00"), captor.getValue().getAmount());
        assertEquals(LocalDate.of(2026, 2, 11), captor.getValue().getDueDate());
    }

    private RentService serviceAt(int year, int month, int day) {
        Clock fixed = Clock.fixed(ZonedDateTime.of(year, month, day, 10, 0, 0, 0, IST).toInstant(), IST);
        return new RentService(rentPaymentRepository, leaseRepository, userRepository, fixed);
    }

    private Lease lease(LocalDate startDate) {
        Property property = new Property();
        property.setId(3L);
        property.setOwnerId(4L);

        Lease lease = new Lease();
        lease.setId(12L);
        lease.setProperty(property);
        lease.setStartDate(startDate);
        lease.setMonthlyRent(new BigDecimal("18000.00"));
        lease.setFineGraceDays(5);
        lease.setFinePerDay(new BigDecimal("100.00"));
        return lease;
    }

    private RentPayment payment(Long id, Lease lease, LocalDate dueDate, RentStatus status) {
        RentPayment rent = new RentPayment();
        rent.setId(id);
        rent.setLease(lease);
        rent.setForMonth("2026-01");
        rent.setAmount(lease.getMonthlyRent());
        rent.setDueDate(dueDate);
        rent.setFine(BigDecimal.ZERO);
        rent.setStatus(status);
        return rent;
    }
}

