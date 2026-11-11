package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.LeaseStatus;
import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.LeaseService;
import Tenant.Owner.Management.TOM.Service.RentService;

@ExtendWith(MockitoExtension.class)
class LeaseAmenitiesTest {
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
    void tenantLeaseResponseIncludesSortedAmenitiesFromActiveLease() {
        User tenant = new User();
        tenant.setId(7L);
        tenant.setRole(UserRole.TENANT);
        tenant.setName("Tenant Seven");
        User owner = new User();
        owner.setId(11L);
        owner.setName("Owner Eleven");
        owner.setEmail("owner@example.com");
        owner.setPhone("555-0100");
        Property property = new Property();
        property.setId(3L);
        property.setOwnerId(11L);
        property.setTitle("Flat 3");
        property.setAmenities(new HashSet<>(Set.of("Wi-Fi", "Parking")));
        Lease lease = new Lease();
        lease.setId(5L);
        lease.setTenant(tenant);
        lease.setProperty(property);
        lease.setStatus(LeaseStatus.ACTIVE);
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(userRepository.findAllById(List.of(11L))).thenReturn(List.of(owner));
        when(leaseRepository.findTenantLeasesWithDetails(7L, LeaseStatus.ACTIVE))
                .thenReturn(List.of(lease));

        var response = leaseService.getMyActiveLeases(7L);

        assertEquals(1, response.size());
        assertEquals(List.of("Parking", "Wi-Fi"), response.get(0).getAmenities());
        assertEquals("Owner Eleven", response.get(0).getOwnerName());
        assertEquals("owner@example.com", response.get(0).getOwnerEmail());
        assertEquals("555-0100", response.get(0).getOwnerPhone());
        verify(leaseRepository).findTenantLeasesWithDetails(7L, LeaseStatus.ACTIVE);
    }

    @Test
    void nonTenantCannotReadTenantLeaseView() {
        User owner = new User();
        owner.setId(7L);
        owner.setRole(UserRole.OWNER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));

        assertThrows(ResponseStatusException.class, () -> leaseService.getMyActiveLeases(7L));
    }
}