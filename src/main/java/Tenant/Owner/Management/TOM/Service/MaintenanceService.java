package Tenant.Owner.Management.TOM.Service;

import java.time.Clock;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.LeaseStatus;
import Tenant.Owner.Management.TOM.Constants.MaintenancePriority;
import Tenant.Owner.Management.TOM.Constants.MaintenanceStatus;
import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.DTO.CommentResponse;
import Tenant.Owner.Management.TOM.DTO.MaintenanceRequestCreate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceRequestUpdate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceResponse;
import Tenant.Owner.Management.TOM.Entity.MaintenanceComment;
import Tenant.Owner.Management.TOM.Entity.MaintenanceRequest;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceCommentRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceRequestRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@Service
public class MaintenanceService {
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceService.class);
    private final UserRepository userRepository;
    private final MaintenanceCommentRepository maintenanceCommentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;
    private final Clock clock;

    public MaintenanceService(UserRepository userRepository, MaintenanceRequestRepository maintenanceRequestRepository,
            MaintenanceCommentRepository maintenanceCommentRepository, PropertyRepository propertyRepository,
            LeaseRepository leaseRepository, Clock clock) {
        this.userRepository = userRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.maintenanceCommentRepository = maintenanceCommentRepository;
        this.propertyRepository = propertyRepository;
        this.leaseRepository = leaseRepository;
        this.clock = clock;
    }

    @Transactional
    public MaintenanceResponse raiseRequest(MaintenanceRequestCreate request, Long tenantId) {
        validateText(request.getTitle(), "title");
        validateText(request.getDescription(), "description");
        MaintenancePriority priority = parsePriority(request.getPriority());

        User tenant = requireUser(tenantId);
        requireRole(tenant, UserRole.TENANT);
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new NoSuchElementException("Property not found: " + request.getPropertyId()));

        if (!leaseRepository.existsByPropertyIdAndTenantIdAndStatus(
                property.getId(), tenantId, LeaseStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "An active lease for this property is required to raise a maintenance request");
        }

        MaintenanceRequest maintenanceRequest = new MaintenanceRequest();
        maintenanceRequest.setProperty(property);
        maintenanceRequest.setTenant(tenant);
        maintenanceRequest.setTitle(request.getTitle().trim());
        maintenanceRequest.setDescription(request.getDescription().trim());
        maintenanceRequest.setPriority(priority);
        maintenanceRequest.setStatus(MaintenanceStatus.OPEN);
        maintenanceRequest.setCreatedAt(LocalDateTime.now(clock));

        return toResponse(maintenanceRequestRepository.save(maintenanceRequest));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getPropertyRequests(Long propertyId, Long ownerId, Long tenantId) {
        if ((ownerId == null) == (tenantId == null)) {
            throw new IllegalArgumentException("Provide exactly one of ownerId or tenantId");
        }
        if (ownerId != null) {
            User owner = requireUser(ownerId);
            requireRole(owner, UserRole.OWNER);
            propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                    .orElseThrow(() -> new NoSuchElementException("Property not found: " + propertyId));
            return maintenanceRequestRepository.findByPropertyIdAndProperty_OwnerId(propertyId, ownerId)
                    .stream().map(this::toResponse).toList();
        }

        User tenant = requireUser(tenantId);
        requireRole(tenant, UserRole.TENANT);
        return maintenanceRequestRepository.findByPropertyIdAndTenantId(propertyId, tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getRequestsPage(Long ownerId, Long tenantId, Pageable pageable,
            String search, Long propertyId) {
        if ((ownerId == null) == (tenantId == null)) {
            throw new IllegalArgumentException("Provide exactly one of ownerId or tenantId");
        }
        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim();
        if (ownerId != null) {
            User owner = requireUser(ownerId);
            requireRole(owner, UserRole.OWNER);
            Page<MaintenanceResponse> result = maintenanceRequestRepository
                    .findOwnerRequests(ownerId, normalizedSearch, propertyId, pageable)
                    .map(this::toResponse);
            logger.info("Maintenance owner query completed: ownerId={} page={} totalElements={}",
                    ownerId, pageable.getPageNumber(), result.getTotalElements());
            return result;
        }
        User tenant = requireUser(tenantId);
        requireRole(tenant, UserRole.TENANT);
        Page<MaintenanceResponse> result = maintenanceRequestRepository
                .findTenantRequests(tenantId, normalizedSearch, propertyId, pageable)
                .map(this::toResponse);
        logger.info("Maintenance tenant query completed: tenantId={} page={} totalElements={}",
                tenantId, pageable.getPageNumber(), result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse getRequest(Long requestId, Long ownerId, Long tenantId) {
        return toResponse(findAccessibleRequest(requestId, ownerId, tenantId));
    }

    @Transactional
    public MaintenanceResponse updateRequest(Long requestId, MaintenanceRequestUpdate update, Long tenantId) {
        User tenant = requireUser(tenantId);
        requireRole(tenant, UserRole.TENANT);
        MaintenanceRequest request = maintenanceRequestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));
        if (MaintenanceStatus.RESOLVED == request.getStatus()
                || MaintenanceStatus.CANCELLED == request.getStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A resolved or cancelled maintenance request cannot be edited");
        }
        validateText(update.getTitle(), "title");
        validateText(update.getDescription(), "description");
        MaintenancePriority priority = parsePriority(update.getPriority());
        request.setTitle(update.getTitle().trim());
        request.setDescription(update.getDescription().trim());
        request.setPriority(priority);
        return toResponse(maintenanceRequestRepository.save(request));
    }

    @Transactional
    public MaintenanceResponse cancelRequest(Long requestId, Long tenantId) {
        User tenant = requireUser(tenantId);
        requireRole(tenant, UserRole.TENANT);
        MaintenanceRequest request = maintenanceRequestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));
        if (MaintenanceStatus.OPEN != request.getStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A maintenance request can only be cancelled while it is open");
        }
        request.setStatus(MaintenanceStatus.CANCELLED);
        return toResponse(maintenanceRequestRepository.save(request));
    }

    @Transactional
    public MaintenanceResponse updateStatus(Long requestId, String status, Long ownerId) {
        User owner = requireUser(ownerId);
        requireRole(owner, UserRole.OWNER);
        MaintenanceRequest request = maintenanceRequestRepository.findByIdAndProperty_OwnerId(requestId, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));

        MaintenanceStatus nextStatus;
        try {
            nextStatus = MaintenanceStatus.valueOf(status == null ? "" : status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid maintenance status transition: "
                    + request.getStatus() + " -> " + status);
        }

        boolean validTransition = (MaintenanceStatus.OPEN == request.getStatus()
                && (MaintenanceStatus.IN_PROGRESS == nextStatus || MaintenanceStatus.RESOLVED == nextStatus))
                || (MaintenanceStatus.IN_PROGRESS == request.getStatus()
                        && MaintenanceStatus.RESOLVED == nextStatus);
        if (!validTransition) {
            throw new IllegalArgumentException("Invalid maintenance status transition: "
                    + request.getStatus() + " -> " + nextStatus);
        }

        request.setStatus(nextStatus);
        request.setResolvedAt(MaintenanceStatus.RESOLVED == nextStatus ? LocalDateTime.now(clock) : null);
        return toResponse(maintenanceRequestRepository.save(request));
    }

    @Transactional
    public CommentResponse addComment(Long requestId, String commentText, Long ownerId, Long tenantId) {
        validateText(commentText, "comment");
        MaintenanceRequest request = findAccessibleRequest(requestId, ownerId, tenantId);
        User author = ownerId != null ? requireUser(ownerId) : requireUser(tenantId);

        MaintenanceComment comment = new MaintenanceComment();
        comment.setRequest(request);
        comment.setUser(author);
        comment.setComment(commentText.trim());
        comment.setCreatedAt(LocalDateTime.now(clock));
        return toCommentResponse(maintenanceCommentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long requestId, Long ownerId, Long tenantId) {
        MaintenanceRequest request = findAccessibleRequest(requestId, ownerId, tenantId);
        return maintenanceCommentRepository.findByRequestIdOrderByCreatedAtAsc(request.getId())
                .stream().map(this::toCommentResponse).toList();
    }

    private MaintenanceRequest findAccessibleRequest(Long requestId, Long ownerId, Long tenantId) {
        if ((ownerId == null) == (tenantId == null)) {
            throw new IllegalArgumentException("Provide exactly one of ownerId or tenantId");
        }
        if (ownerId != null) {
            User owner = requireUser(ownerId);
            requireRole(owner, UserRole.OWNER);
            return maintenanceRequestRepository.findByIdAndProperty_OwnerId(requestId, ownerId)
                    .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));
        }

        User tenant = requireUser(tenantId);
        requireRole(tenant, UserRole.TENANT);
        return maintenanceRequestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    private void requireRole(User user, UserRole requiredRole) {
        if (requiredRole != user.getRole()) {
            logger.warn("Maintenance access rejected: userId={} actualRole={} requiredRole={}",
                    user.getId(), user.getRole(), requiredRole);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This action is not allowed for this user role");
        }
    }

    private void validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private MaintenancePriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("Priority must be LOW, MEDIUM, or HIGH");
        }
        try {
            return MaintenancePriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Priority must be LOW, MEDIUM, or HIGH");
        }
    }

    private MaintenanceResponse toResponse(MaintenanceRequest request) {
        MaintenanceResponse response = new MaintenanceResponse();
        response.setId(request.getId());
        response.setPropertyId(request.getProperty().getId());
        response.setPropertyTitle(request.getProperty().getTitle());
        response.setTenantId(request.getTenant().getId());
        response.setTenantName(request.getTenant().getName());
        response.setTitle(request.getTitle());
        response.setDescription(request.getDescription());
        response.setPriority(request.getPriority() != null ? request.getPriority().name() : null);
        response.setStatus(request.getStatus() != null ? request.getStatus().name() : null);
        response.setCreatedAt(request.getCreatedAt());
        response.setResolvedAt(request.getResolvedAt());
        return response;
    }

    private CommentResponse toCommentResponse(MaintenanceComment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setRequestId(comment.getRequest().getId());
        response.setUserId(comment.getUser().getId());
        response.setUserName(comment.getUser().getName());
        response.setUserRole(comment.getUser().getRole() != null ? comment.getUser().getRole().name() : null);
        response.setComment(comment.getComment());
        response.setCreatedAt(comment.getCreatedAt());
        return response;

    }

}