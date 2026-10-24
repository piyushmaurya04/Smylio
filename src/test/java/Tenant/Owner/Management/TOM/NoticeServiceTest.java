package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.DTO.NoticeCreateRequest;
import Tenant.Owner.Management.TOM.Entity.Notice;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.NoticeRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.NoticeService;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    void ownerCanCreateBroadcastNotice() {
        User owner = user(4L, Constants.ROLE_OWNER);
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setTitle("Water shutoff");
        request.setMessage("Water will be unavailable tomorrow morning.");
        when(userRepository.findById(4L)).thenReturn(Optional.of(owner));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            notice.setId(10L);
            return notice;
        });

        var response = noticeService.create(request, 4L);

        assertEquals(10L, response.getId());
        assertEquals(4L, response.getOwnerId());
        assertNull(response.getPropertyId());
        assertEquals("Water shutoff", response.getTitle());
    }

    @Test
    void ownerCannotTargetAnotherOwnersProperty() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setPropertyId(8L);
        request.setTitle("Notice");
        request.setMessage("Message");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(4L, Constants.ROLE_OWNER)));
        when(propertyRepository.findByIdAndOwnerId(8L, 4L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> noticeService.create(request, 4L));
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    void tenantReadsOnlyNoticesForActiveLeasedPropertiesAndTheirOwners() {
        User tenant = user(7L, Constants.ROLE_TENANT);
        Notice notice = new Notice();
        notice.setOwner(user(4L, Constants.ROLE_OWNER));
        notice.setTitle("Building maintenance");
        notice.setMessage("Elevator maintenance Friday.");
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(noticeRepository.findVisibleToTenant(7L, Constants.LEASE_ACTIVE)).thenReturn(List.of(notice));

        var notices = noticeService.getNotices(null, 7L);

        assertEquals(1, notices.size());
        assertEquals("Building maintenance", notices.get(0).getTitle());
        verify(noticeRepository).findVisibleToTenant(7L, Constants.LEASE_ACTIVE);
    }

    @Test
    void ownerCanReadFilteredNoticePage() {
        User owner = user(4L, Constants.ROLE_OWNER);
        var pageable = PageRequest.of(1, 5, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(userRepository.findById(4L)).thenReturn(Optional.of(owner));
        when(noticeRepository.findByOwnerWithFilters(4L, "water", 8L, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 6));

        var result = noticeService.getNoticesPage(4L, null, pageable, " water ", 8L);

        assertEquals(6, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        verify(noticeRepository).findByOwnerWithFilters(4L, "water", 8L, pageable);
    }

    @Test
    void tenantCanReadPagedNoticesForActiveLeases() {
        User tenant = user(7L, Constants.ROLE_TENANT);
        var pageable = PageRequest.of(0, 5);
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(noticeRepository.findVisibleToTenant(7L, Constants.LEASE_ACTIVE, "", null, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        var result = noticeService.getNoticesPage(null, 7L, pageable, null, null);

        assertEquals(0, result.getTotalElements());
        verify(noticeRepository).findVisibleToTenant(7L, Constants.LEASE_ACTIVE, "", null, pageable);
    }

    @Test
    void tenantNoticePagePassesSearchAndPropertyFilters() {
        User tenant = user(7L, Constants.ROLE_TENANT);
        var pageable = PageRequest.of(0, 5);
        when(userRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(noticeRepository.findVisibleToTenant(7L, Constants.LEASE_ACTIVE, "water", 8L, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        var result = noticeService.getNoticesPage(null, 7L, pageable, " water ", 8L);

        assertEquals(0, result.getTotalElements());
        verify(noticeRepository).findVisibleToTenant(7L, Constants.LEASE_ACTIVE, "water", 8L, pageable);
    }

    @Test
    void callerMustProvideExactlyOneIdentity() {
        assertThrows(IllegalArgumentException.class, () -> noticeService.getNotices(null, null));
        assertThrows(IllegalArgumentException.class, () -> noticeService.getNotices(4L, 7L));
    }

    @Test
    void tenantCannotPostNotice() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setTitle("Notice");
        request.setMessage("Message");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, Constants.ROLE_TENANT)));

        assertThrows(ResponseStatusException.class, () -> noticeService.create(request, 7L));
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    void ownerCanUpdateOwnNotice() {
        User owner = user(4L, Constants.ROLE_OWNER);
        Notice notice = new Notice();
        notice.setId(10L);
        notice.setOwner(owner);
        notice.setTitle("Old title");
        notice.setMessage("Old message");
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setTitle(" Updated title ");
        request.setMessage(" Updated message ");
        when(userRepository.findById(4L)).thenReturn(Optional.of(owner));
        when(noticeRepository.findByIdAndOwner_Id(10L, 4L)).thenReturn(Optional.of(notice));
        when(noticeRepository.save(notice)).thenReturn(notice);

        var response = noticeService.update(10L, request, 4L);

        assertEquals("Updated title", response.getTitle());
        assertEquals("Updated message", response.getMessage());
        verify(noticeRepository).save(notice);
    }

    @Test
    void ownerCannotUpdateAnotherOwnersNotice() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setTitle("Updated title");
        request.setMessage("Updated message");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(4L, Constants.ROLE_OWNER)));
        when(noticeRepository.findByIdAndOwner_Id(10L, 4L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> noticeService.update(10L, request, 4L));
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    void ownerCannotUpdateNoticeWithAnotherOwnersProperty() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setTitle("Updated title");
        request.setMessage("Updated message");
        request.setPropertyId(8L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(4L, Constants.ROLE_OWNER)));
        when(noticeRepository.findByIdAndOwner_Id(10L, 4L)).thenReturn(Optional.of(new Notice()));
        when(propertyRepository.findByIdAndOwnerId(8L, 4L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> noticeService.update(10L, request, 4L));
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    void tenantCannotUpdateNotice() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setTitle("Updated title");
        request.setMessage("Updated message");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, Constants.ROLE_TENANT)));

        assertThrows(ResponseStatusException.class, () -> noticeService.update(10L, request, 7L));
        verify(noticeRepository, never()).findByIdAndOwner_Id(10L, 7L);
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    void ownerCanDeleteOwnNotice() {
        User owner = user(4L, Constants.ROLE_OWNER);
        Notice notice = new Notice();
        notice.setOwner(owner);
        when(userRepository.findById(4L)).thenReturn(Optional.of(owner));
        when(noticeRepository.findByIdAndOwner_Id(10L, 4L)).thenReturn(Optional.of(notice));

        noticeService.delete(10L, 4L);

        verify(noticeRepository).delete(notice);
    }

    @Test
    void ownerCannotDeleteAnotherOwnersNotice() {
        User owner = user(4L, Constants.ROLE_OWNER);
        when(userRepository.findById(4L)).thenReturn(Optional.of(owner));
        when(noticeRepository.findByIdAndOwner_Id(10L, 4L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> noticeService.delete(10L, 4L));
        verify(noticeRepository, never()).delete(any(Notice.class));
    }

    @Test
    void tenantCannotDeleteNotice() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, Constants.ROLE_TENANT)));

        assertThrows(ResponseStatusException.class, () -> noticeService.delete(10L, 7L));
        verify(noticeRepository, never()).findByIdAndOwner_Id(10L, 7L);
        verify(noticeRepository, never()).delete(any(Notice.class));
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setName("User " + id);
        return user;
    }
}