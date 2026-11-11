package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import Tenant.Owner.Management.TOM.Constants.LeaseStatus;
import Tenant.Owner.Management.TOM.Constants.PropertyStatus;
import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.DTO.LeaseRequest;
import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.LeaseService;
import Tenant.Owner.Management.TOM.Service.RentService;

@ExtendWith(MockitoExtension.class)
class LeaseServiceTest {
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RentService rentService;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-03-15T04:30:00Z"), ZoneId.of("Asia/Kolkata"));

    @InjectMocks
    private LeaseService leaseService;

    @Test
    void createLocksPropertyNormalizesCodeAndSetsOccupied() {
        Property property = new Property();
        property.setId(3L);
        property.setOwnerId(11L);
        property.setTitle("Flat 3");
        property.setStatus(PropertyStatus.AVAILABLE);

        User owner = new User();
        owner.setId(11L);
        owner.setName("Owner");
        owner.setEmail("owner@example.test");
        owner.setPhone("5550100");
        owner.setRole(UserRole.OWNER);

        User tenant = new User();
        tenant.setId(7L);
        tenant.setName("Tenant");
        tenant.setRole(UserRole.TENANT);
        tenant.setTenantCode("TNT-ABC123");

        LeaseRequest request = validRequest();
        request.setTenantCode(" tnt-abc123 ");

        when(propertyRepository.findByIdAndOwnerIdForUpdate(3L, 11L)).thenReturn(Optional.of(property));
        when(leaseRepository.existsByPropertyIdAndStatus(3L, LeaseStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findByTenantCode("TNT-ABC123")).thenReturn(Optional.of(tenant));
        when(userRepository.findById(11L)).thenReturn(Optional.of(owner));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> {
            Lease saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        var response = leaseService.create(request, 11L);

        assertEquals(9L, response.getId());
        assertEquals(PropertyStatus.OCCUPIED, property.getStatus());
        verify(propertyRepository).findByIdAndOwnerIdForUpdate(3L, 11L);
        verify(userRepository).findByTenantCode("TNT-ABC123");
    }

    @Test
    void createRejectsWhenLockedPropertyAlreadyOccupied() {
        Property property = new Property();
        property.setId(3L);
        property.setOwnerId(11L);
        property.setStatus(PropertyStatus.OCCUPIED);

        when(propertyRepository.findByIdAndOwnerIdForUpdate(3L, 11L)).thenReturn(Optional.of(property));

        assertThrows(IllegalStateException.class, () -> leaseService.create(validRequest(), 11L));
        verify(leaseRepository, never()).save(any());
    }

    @Test
    void createRejectsNonTenantUser() {
        Property property = new Property();
        property.setId(3L);
        property.setOwnerId(11L);
        property.setStatus(PropertyStatus.AVAILABLE);

        User ownerAccount = new User();
        ownerAccount.setId(8L);
        ownerAccount.setRole(UserRole.OWNER);
        ownerAccount.setTenantCode("TNT-OWNER");

        when(propertyRepository.findByIdAndOwnerIdForUpdate(3L, 11L)).thenReturn(Optional.of(property));
        when(leaseRepository.existsByPropertyIdAndStatus(3L, LeaseStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findByTenantCode("TNT-OWNER")).thenReturn(Optional.of(ownerAccount));

        LeaseRequest request = validRequest();
        request.setTenantCode("TNT-OWNER");

        assertThrows(java.util.NoSuchElementException.class, () -> leaseService.create(request, 11L));
        verify(leaseRepository, never()).save(any());
    }

    @Test
    void updateRejectsEndedLease() {
        Lease ended = new Lease();
        ended.setId(5L);
        ended.setStatus(LeaseStatus.ENDED);
        when(leaseRepository.findByIdAndProperty_OwnerId(5L, 11L)).thenReturn(Optional.of(ended));

        assertThrows(IllegalStateException.class, () -> leaseService.leaseUpdate(5L, validRequest(), 11L));
        verify(leaseRepository, never()).save(any());
    }

    @Test
    void endLeaseUsesClockDateTriggersProratedRentAndFreesProperty() {
        Property property = new Property();
        property.setId(3L);
        property.setOwnerId(11L);
        property.setTitle("Flat 3");
        property.setStatus(PropertyStatus.OCCUPIED);

        User owner = new User();
        owner.setId(11L);
        owner.setName("Owner");
        owner.setEmail("owner@example.test");
        owner.setPhone("5550100");

        User tenant = new User();
        tenant.setId(7L);
        tenant.setName("Tenant");
        tenant.setTenantCode("TNT-ABC123");

        Lease active = new Lease();
        active.setId(5L);
        active.setProperty(property);
        active.setTenant(tenant);
        active.setStartDate(LocalDate.of(2026, 2, 1));
        active.setMonthlyRent(new BigDecimal("15000.00"));
        active.setFineGraceDays(5);
        active.setFinePerDay(new BigDecimal("100.00"));
        active.setStatus(LeaseStatus.ACTIVE);

        when(leaseRepository.findByIdAndProperty_OwnerId(5L, 11L)).thenReturn(Optional.of(active));
        when(leaseRepository.save(active)).thenReturn(active);
        when(userRepository.findById(11L)).thenReturn(Optional.of(owner));

        var response = leaseService.endLease(5L, 11L);

        assertEquals(LeaseStatus.ENDED.name(), response.getStatus());
        assertEquals(LocalDate.of(2026, 3, 15), response.getEndDate());
        assertEquals(PropertyStatus.AVAILABLE, property.getStatus());
        verify(rentService).generateFinalRentOnLeaseEnd(active, LocalDate.of(2026, 3, 15));
    }

    private LeaseRequest validRequest() {
        LeaseRequest request = new LeaseRequest();
        request.setPropertyId(3L);
        request.setTenantCode("TNT-OWNER");
        request.setStartDate(LocalDate.of(2026, 1, 12));
        request.setMonthlyRent(new BigDecimal("15000.00"));
        request.setFineGraceDays(5);
        request.setFinePerDay(new BigDecimal("100.00"));
        return request;
    }
}

