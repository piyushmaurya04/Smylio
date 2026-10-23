package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.Entity.Lease;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.LeaseService;

@ExtendWith(MockitoExtension.class)
class LeaseAmenitiesTest {
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaseService leaseService;

    @Test
    void tenantLeaseResponseIncludesSortedAmenitiesFromActiveLease() {
        User tenant = new User();
        tenant.setId(7L);
        tenant.setRole(Constants.ROLE_TENANT);
        tenant.setName("Tenant Seven");
        Property property = new Property();
        property.setId(3L);
        property.setTitle("Flat 3");
        property.setAmenities(new HashSet<>(Set.of("Wi-Fi", "Parking")));
        Lease lease = new Lease();
        lease.setId(5L);
        lease.setTenant(tenant);
        lease.setProperty(property);
        lease.setStatus(Constants.LEASE_ACTIVE);
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(leaseRepository.findTenantLeasesWithDetails(7L, Constants.LEASE_ACTIVE))
                .thenReturn(List.of(lease));

        var response = leaseService.getMyActiveLeases(7L);

        assertEquals(1, response.size());
        assertEquals(List.of("Parking", "Wi-Fi"), response.get(0).getAmenities());
        verify(leaseRepository).findTenantLeasesWithDetails(7L, Constants.LEASE_ACTIVE);
    }

    @Test
    void nonTenantCannotReadTenantLeaseView() {
        User owner = new User();
        owner.setId(7L);
        owner.setRole(Constants.ROLE_OWNER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));

        assertThrows(ResponseStatusException.class, () -> leaseService.getMyActiveLeases(7L));
    }
}