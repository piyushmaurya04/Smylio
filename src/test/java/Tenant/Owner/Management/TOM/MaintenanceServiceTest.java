package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.LeaseStatus;
import Tenant.Owner.Management.TOM.Constants.MaintenancePriority;
import Tenant.Owner.Management.TOM.Constants.MaintenanceStatus;
import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.DTO.MaintenanceRequestCreate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceRequestUpdate;
import Tenant.Owner.Management.TOM.Entity.MaintenanceRequest;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceCommentRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceRequestRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.MaintenanceService;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock
    private MaintenanceCommentRepository maintenanceCommentRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-03-15T04:30:00Z"), ZoneId.of("Asia/Kolkata"));

    @InjectMocks
    private MaintenanceService maintenanceService;

    @Test
    void raiseRequestCreatesOpenRequestForTenantWithActiveLease() {
        User tenant = tenant(7L);
        Property property = property(3L);
        MaintenanceRequestCreate input = new MaintenanceRequestCreate();
        input.setPropertyId(3L);
        input.setTitle("Leaking tap");
        input.setDescription("The kitchen tap is leaking");
        input.setPriority("high");

        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(propertyRepository.findById(3L)).thenReturn(Optional.of(property));
        when(leaseRepository.existsByPropertyIdAndTenantIdAndStatus(3L, 7L, LeaseStatus.ACTIVE))
                .thenReturn(true);
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(invocation -> {
            MaintenanceRequest request = invocation.getArgument(0);
            request.setId(11L);
            return request;
        });

        var response = maintenanceService.raiseRequest(input, 7L);

        assertEquals(11L, response.getId());
        assertEquals(3L, response.getPropertyId());
        assertEquals(7L, response.getTenantId());
        assertEquals("HIGH", response.getPriority());
        assertEquals(MaintenanceStatus.OPEN.name(), response.getStatus());
    }

    @Test
    void raiseRequestRejectsTenantWithoutActiveLease() {
        User tenant = tenant(7L);
        Property property = property(3L);
        MaintenanceRequestCreate input = new MaintenanceRequestCreate();
        input.setPropertyId(3L);
        input.setTitle("Leaking tap");
        input.setDescription("The kitchen tap is leaking");
        input.setPriority("HIGH");

        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(propertyRepository.findById(3L)).thenReturn(Optional.of(property));
        when(leaseRepository.existsByPropertyIdAndTenantIdAndStatus(3L, 7L, LeaseStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> maintenanceService.raiseRequest(input, 7L));
        verify(maintenanceRequestRepository, never()).save(any(MaintenanceRequest.class));
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        User owner = new User();
        owner.setId(2L);
        owner.setRole(UserRole.OWNER);
        MaintenanceRequest request = new MaintenanceRequest();
        request.setStatus(MaintenanceStatus.OPEN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(maintenanceRequestRepository.findByIdAndProperty_OwnerId(11L, 2L))
                .thenReturn(Optional.of(request));

        assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.updateStatus(11L, MaintenanceStatus.OPEN.name(), 2L));
        verify(maintenanceRequestRepository, never()).save(any(MaintenanceRequest.class));
    }

    @Test
    void tenantCannotCommentOnAnotherTenantsRequest() {
        when(userRepository.findById(8L)).thenReturn(Optional.of(tenant(8L)));
        when(maintenanceRequestRepository.findByIdAndTenantId(11L, 8L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> maintenanceService.addComment(11L, "I can help", null, 8L));
        verify(maintenanceCommentRepository, never()).save(any());
    }

    @Test
    void tenantCanUpdateOwnRequestDetailsWithoutChangingStatus() {
        User tenant = tenant(7L);
        MaintenanceRequest request = request(11L, property(3L), tenant(7L));
        request.setStatus(MaintenanceStatus.IN_PROGRESS);
        MaintenanceRequestUpdate update = new MaintenanceRequestUpdate();
        update.setTitle("  Fixed leak  ");
        update.setDescription("  Replaced the kitchen tap  ");
        update.setPriority("high");

        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(maintenanceRequestRepository.findByIdAndTenantId(11L, 7L)).thenReturn(Optional.of(request));
        when(maintenanceRequestRepository.save(request)).thenReturn(request);

        var response = maintenanceService.updateRequest(11L, update, 7L);

        assertEquals("Fixed leak", response.getTitle());
        assertEquals("Replaced the kitchen tap", response.getDescription());
        assertEquals("HIGH", response.getPriority());
        assertEquals(MaintenanceStatus.IN_PROGRESS.name(), response.getStatus());
    }

    @Test
    void tenantCannotUpdateAnotherTenantsRequest() {
        when(userRepository.findById(8L)).thenReturn(Optional.of(tenant(8L)));
        when(maintenanceRequestRepository.findByIdAndTenantId(11L, 8L)).thenReturn(Optional.empty());
        MaintenanceRequestUpdate update = new MaintenanceRequestUpdate();
        update.setTitle("Changed");
        update.setDescription("Changed");
        update.setPriority("LOW");

        assertThrows(java.util.NoSuchElementException.class,
                () -> maintenanceService.updateRequest(11L, update, 8L));
        verify(maintenanceRequestRepository, never()).save(any(MaintenanceRequest.class));
    }

    @Test
    void tenantCannotEditResolvedRequest() {
        User tenant = tenant(7L);
        MaintenanceRequest request = request(11L, property(3L), tenant);
        request.setStatus(MaintenanceStatus.RESOLVED);
        MaintenanceRequestUpdate update = new MaintenanceRequestUpdate();
        update.setTitle("Changed");
        update.setDescription("Changed");
        update.setPriority("LOW");
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(maintenanceRequestRepository.findByIdAndTenantId(11L, 7L)).thenReturn(Optional.of(request));

        assertThrows(ResponseStatusException.class,
                () -> maintenanceService.updateRequest(11L, update, 7L));
        verify(maintenanceRequestRepository, never()).save(any(MaintenanceRequest.class));
    }

    @Test
    void tenantCannotEditCancelledRequest() {
        User tenant = tenant(7L);
        MaintenanceRequest request = request(11L, property(3L), tenant);
        request.setStatus(MaintenanceStatus.CANCELLED);
        MaintenanceRequestUpdate update = new MaintenanceRequestUpdate();
        update.setTitle("Changed");
        update.setDescription("Changed");
        update.setPriority("LOW");
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(maintenanceRequestRepository.findByIdAndTenantId(11L, 7L)).thenReturn(Optional.of(request));

        assertThrows(ResponseStatusException.class,
                () -> maintenanceService.updateRequest(11L, update, 7L));
        verify(maintenanceRequestRepository, never()).save(any(MaintenanceRequest.class));
    }

    @Test
    void commentResponseIncludesAuthorRole() {
        User owner = owner(2L);
        MaintenanceRequest request = request(11L, property(3L), tenant(7L));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(maintenanceRequestRepository.findByIdAndProperty_OwnerId(11L, 2L))
                .thenReturn(Optional.of(request));
        when(maintenanceCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = maintenanceService.addComment(11L, "Plumber scheduled", 2L, null);

        assertEquals(UserRole.OWNER.name(), response.getUserRole());
    }

    @Test
    void tenantCanCancelOwnOpenRequestWithoutDeletingIt() {
        User tenant = tenant(7L);
        MaintenanceRequest request = request(11L, property(3L), tenant(7L));
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(maintenanceRequestRepository.findByIdAndTenantId(11L, 7L)).thenReturn(Optional.of(request));
        when(maintenanceRequestRepository.save(request)).thenReturn(request);

        var response = maintenanceService.cancelRequest(11L, 7L);

        assertEquals(MaintenanceStatus.CANCELLED.name(), response.getStatus());
        verify(maintenanceRequestRepository).save(request);
        verify(maintenanceRequestRepository, never()).delete(any());
    }

    @Test
    void tenantCannotCancelRequestAfterItLeavesOpenStatus() {
        User tenant = tenant(7L);
        MaintenanceRequest request = request(11L, property(3L), tenant);
        request.setStatus(MaintenanceStatus.IN_PROGRESS);
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(maintenanceRequestRepository.findByIdAndTenantId(11L, 7L)).thenReturn(Optional.of(request));

        assertThrows(ResponseStatusException.class, () -> maintenanceService.cancelRequest(11L, 7L));
        verify(maintenanceRequestRepository, never()).save(any(MaintenanceRequest.class));
    }

    @Test
    void ownerPageSearchIsScopedAndMapped() {
        User owner = owner(2L);
        MaintenanceRequest request = request(11L, property(3L), tenant(7L));
        var pageable = PageRequest.of(0, 5);
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(maintenanceRequestRepository.findOwnerRequests(2L, "leak", 3L, pageable))
                .thenReturn(new PageImpl<>(List.of(request), pageable, 1));

        var result = maintenanceService.getRequestsPage(2L, null, pageable, " leak ", 3L);

        assertEquals(1, result.getTotalElements());
        assertEquals(11L, result.getContent().get(0).getId());
        assertEquals("Tenant 7", result.getContent().get(0).getTenantName());
    }

    private User owner(Long id) {
        User owner = new User();
        owner.setId(id);
        owner.setName("Owner " + id);
        owner.setRole(UserRole.OWNER);
        return owner;
    }

    private MaintenanceRequest request(Long id, Property property, User tenant) {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setId(id);
        request.setProperty(property);
        request.setTenant(tenant);
        request.setTitle("Leaking tap");
        request.setDescription("Kitchen tap");
        request.setPriority(MaintenancePriority.MEDIUM);
        request.setStatus(MaintenanceStatus.OPEN);
        return request;
    }

    private User tenant(Long id) {
        User tenant = new User();
        tenant.setId(id);
        tenant.setName("Tenant " + id);
        tenant.setRole(UserRole.TENANT);
        return tenant;
    }

    private Property property(Long id) {
        Property property = new Property();
        property.setId(id);
        property.setTitle("Flat " + id);
        return property;
    }
}