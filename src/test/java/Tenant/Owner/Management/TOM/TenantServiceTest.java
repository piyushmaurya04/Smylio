package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}