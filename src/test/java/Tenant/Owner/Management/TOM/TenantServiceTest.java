package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.TenantService;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void searchesTenantUsingTrimmedCaseInsensitiveCodeAndReturnsMaskedDto() {
        User tenant = new User();
        tenant.setId(12L);
        tenant.setName("Jordan Tenant");
        tenant.setEmail("jordan@example.test");
        tenant.setPhone("9876543210");
        tenant.setRole(UserRole.TENANT);
        tenant.setTenantCode("TNT-8K3P9M2W");
        tenant.setPassword("must-not-be-returned");
        when(userRepository.findByTenantCode("TNT-8K3P9M2W")).thenReturn(Optional.of(tenant));

        var response = tenantService.findByTenantCode(" tnt-8k3p9m2w ");

        assertEquals("Jordan Tenant", response.getName());
        assertEquals("******3210", response.getMaskedPhone());
        assertEquals("TNT-8K3P9M2W", response.getTenantCode());
        verify(userRepository).findByTenantCode("TNT-8K3P9M2W");
    }

    @Test
    void rejectsBlankCode() {
        assertThrows(IllegalArgumentException.class, () -> tenantService.findByTenantCode("  "));
    }

    @Test
    void doesNotReturnOwnerEvenIfRepositoryReturnsOne() {
        User owner = new User();
        owner.setTenantCode("TNT-1");
        owner.setRole(UserRole.OWNER);
        when(userRepository.findByTenantCode("TNT-1")).thenReturn(Optional.of(owner));

        assertThrows(java.util.NoSuchElementException.class,
                () -> tenantService.findByTenantCode("TNT-1"));
    }

    @Test
    void rejectsSelfSearchWhenCallerUserIdMatchesTenant() {
        User self = new User();
        self.setId(5L);
        self.setName("Self User");
        self.setRole(UserRole.TENANT);
        self.setTenantCode("TNT-SELF");
        when(userRepository.findByTenantCode("TNT-SELF")).thenReturn(Optional.of(self));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> tenantService.findByTenantCode("TNT-SELF", 5L));
        assertTrue(ex.getMessage().contains("This is your own Tenant Code"));
    }

    @Test
    void allowsSearchWhenCallerUserIdDiffersFromTenant() {
        User other = new User();
        other.setId(10L);
        other.setName("Other User");
        other.setRole(UserRole.TENANT);
        other.setPhone("9876543210");
        other.setTenantCode("TNT-OTHER");
        when(userRepository.findByTenantCode("TNT-OTHER")).thenReturn(Optional.of(other));

        var res = tenantService.findByTenantCode("TNT-OTHER", 5L);
        assertNotNull(res);
        assertEquals("Other User", res.getName());
    }
}