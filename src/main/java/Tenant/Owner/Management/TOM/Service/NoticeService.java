package Tenant.Owner.Management.TOM.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.DTO.NoticeCreateRequest;
import Tenant.Owner.Management.TOM.DTO.NoticeResponse;
import Tenant.Owner.Management.TOM.Entity.Notice;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.NoticeRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@Service
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public NoticeService(NoticeRepository noticeRepository, PropertyRepository propertyRepository,
            UserRepository userRepository) {
        this.noticeRepository = noticeRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NoticeResponse create(NoticeCreateRequest request, Long ownerId) {
        validateText(request.getTitle(), "title");
        validateText(request.getMessage(), "message");
        User owner = requireUser(ownerId);
        requireRole(owner, Constants.ROLE_OWNER);

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findByIdAndOwnerId(request.getPropertyId(), ownerId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Property not found: " + request.getPropertyId()));
        }

        Notice notice = new Notice();
        notice.setOwner(owner);
        notice.setProperty(property);
        notice.setTitle(request.getTitle().trim());
        notice.setMessage(request.getMessage().trim());
        notice.setCreatedAt(LocalDateTime.now());
        return toResponse(noticeRepository.save(notice));
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getNotices(Long ownerId, Long tenantId) {
        if ((ownerId == null) == (tenantId == null)) {
            throw new IllegalArgumentException("Provide exactly one of ownerId or tenantId");
        }

        if (ownerId != null) {
            User owner = requireUser(ownerId);
            requireRole(owner, Constants.ROLE_OWNER);
            return noticeRepository.findByOwner_IdOrderByCreatedAtDesc(ownerId)
                    .stream().map(this::toResponse).toList();
        }

        User tenant = requireUser(tenantId);
        requireRole(tenant, Constants.ROLE_TENANT);
        return noticeRepository.findVisibleToTenant(tenantId, Constants.LEASE_ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<NoticeResponse> getNoticesPage(Long ownerId, Long tenantId, Pageable pageable,
            String search, Long propertyId) {
        if ((ownerId == null) == (tenantId == null)) {
            throw new IllegalArgumentException("Provide exactly one of ownerId or tenantId");
        }

        if (ownerId != null) {
            User owner = requireUser(ownerId);
            requireRole(owner, Constants.ROLE_OWNER);
            String searchTerm = search == null || search.isBlank() ? null : search.trim();
            return noticeRepository.findByOwnerWithFilters(ownerId, searchTerm, propertyId, pageable)
                    .map(this::toResponse);
        }

        User tenant = requireUser(tenantId);
        requireRole(tenant, Constants.ROLE_TENANT);
        String searchTerm = search == null || search.isBlank() ? null : search.trim();
        return noticeRepository.findVisibleToTenant(tenantId, Constants.LEASE_ACTIVE, searchTerm, propertyId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public NoticeResponse update(Long noticeId, NoticeCreateRequest request, Long ownerId) {
        validateText(request.getTitle(), "title");
        validateText(request.getMessage(), "message");
        User owner = requireUser(ownerId);
        requireRole(owner, Constants.ROLE_OWNER);
        Notice notice = noticeRepository.findByIdAndOwner_Id(noticeId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notice not found"));

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findByIdAndOwnerId(request.getPropertyId(), ownerId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Property not found: " + request.getPropertyId()));
        }

        notice.setProperty(property);
        notice.setTitle(request.getTitle().trim());
        notice.setMessage(request.getMessage().trim());
        return toResponse(noticeRepository.save(notice));
    }

    @Transactional
    public void delete(Long noticeId, Long ownerId) {
        User owner = requireUser(ownerId);
        requireRole(owner, Constants.ROLE_OWNER);
        Notice notice = noticeRepository.findByIdAndOwner_Id(noticeId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notice not found"));
        noticeRepository.delete(notice);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    private void requireRole(User user, String requiredRole) {
        if (!requiredRole.equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action is not allowed for this user role");
        }
    }

    private void validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private NoticeResponse toResponse(Notice notice) {
        NoticeResponse response = new NoticeResponse();
        response.setId(notice.getId());
        response.setOwnerId(notice.getOwner().getId());
        if (notice.getProperty() != null) {
            response.setPropertyId(notice.getProperty().getId());
            response.setPropertyTitle(notice.getProperty().getTitle());
        }
        response.setTitle(notice.getTitle());
        response.setMessage(notice.getMessage());
        response.setCreatedAt(notice.getCreatedAt());
        return response;
    }
}