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
    private final UserRepository userRepository;
    private final MaintenanceCommentRepository maintenanceCommentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;

    public MaintenanceService(UserRepository userRepository, MaintenanceRequestRepository maintenanceRequestRepository,
            MaintenanceCommentRepository maintenanceCommentRepository, PropertyRepository propertyRepository,
            LeaseRepository leaseRepository) {
        this.userRepository = userRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.maintenanceCommentRepository = maintenanceCommentRepository;
        this.propertyRepository = propertyRepository;
        this.leaseRepository = leaseRepository;
    }

    @Transactional
    public MaintenanceResponse raiseRequest(MaintenanceRequestCreate request, Long tenantId) {
        validateText(request.getTitle(), "title");
        validateText(request.getDescription(), "description");
        validatePriority(request.getPriority());

        User tenant = requireUser(tenantId);
        requireRole(tenant, Constants.ROLE_TENANT);
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new NoSuchElementException("Property not found: " + request.getPropertyId()));

        if (!leaseRepository.existsByPropertyIdAndTenantIdAndStatus(
                property.getId(), tenantId, Constants.LEASE_ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "An active lease for this property is required to raise a maintenance request");
        }

        MaintenanceRequest maintenanceRequest = new MaintenanceRequest();
        maintenanceRequest.setProperty(property);
        maintenanceRequest.setTenant(tenant);
        maintenanceRequest.setTitle(request.getTitle().trim());
        maintenanceRequest.setDescription(request.getDescription().trim());
        maintenanceRequest.setPriority(request.getPriority().trim().toUpperCase());
        maintenanceRequest.setStatus(Constants.MAINTENANCE_OPEN);
        maintenanceRequest.setCreatedAt(LocalDateTime.now());

        return toResponse(maintenanceRequestRepository.save(maintenanceRequest));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getPropertyRequests(Long propertyId, Long ownerId, Long tenantId) {
        if ((ownerId == null) == (tenantId == null)) {
            throw new IllegalArgumentException("Provide exactly one of ownerId or tenantId");
        }
        if (ownerId != null) {
            User owner = requireUser(ownerId);
            requireRole(owner, Constants.ROLE_OWNER);
            propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                    .orElseThrow(() -> new NoSuchElementException("Property not found: " + propertyId));
            return maintenanceRequestRepository.findByPropertyIdAndProperty_OwnerId(propertyId, ownerId)
                    .stream().map(this::toResponse).toList();
        }

        User tenant = requireUser(tenantId);
        requireRole(tenant, Constants.ROLE_TENANT);
        return maintenanceRequestRepository.findByPropertyIdAndTenantId(propertyId, tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getRequestsPage(Long ownerId, Long tenantId, Pageable pageable,
            String search, Long propertyId) {
        if ((ownerId == null) == (tenantId == null)) {
            throw new IllegalArgumentException("Provide exactly one of ownerId or tenantId");
        }
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        if (ownerId != null) {
            User owner = requireUser(ownerId);
            requireRole(owner, Constants.ROLE_OWNER);
            return maintenanceRequestRepository.findOwnerRequests(ownerId, normalizedSearch, propertyId, pageable)
                    .map(this::toResponse);
        }
        User tenant = requireUser(tenantId);
        requireRole(tenant, Constants.ROLE_TENANT);
        return maintenanceRequestRepository.findTenantRequests(tenantId, normalizedSearch, propertyId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse getRequest(Long requestId, Long ownerId, Long tenantId) {
        return toResponse(findAccessibleRequest(requestId, ownerId, tenantId));
    }

    @Transactional
    public MaintenanceResponse updateRequest(Long requestId, MaintenanceRequestUpdate update, Long tenantId) {
        User tenant = requireUser(tenantId);
        requireRole(tenant, Constants.ROLE_TENANT);
        MaintenanceRequest request = maintenanceRequestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));
        if (Constants.MAINTENANCE_RESOLVED.equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A resolved maintenance request cannot be edited");
        }
        validateText(update.getTitle(), "title");
        validateText(update.getDescription(), "description");
        validatePriority(update.getPriority());
        request.setTitle(update.getTitle().trim());
        request.setDescription(update.getDescription().trim());
        request.setPriority(update.getPriority().trim().toUpperCase());
        return toResponse(maintenanceRequestRepository.save(request));
    }

    @Transactional
    public MaintenanceResponse cancelRequest(Long requestId, Long tenantId) {
        User tenant = requireUser(tenantId);
        requireRole(tenant, Constants.ROLE_TENANT);
        MaintenanceRequest request = maintenanceRequestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));
        if (!Constants.MAINTENANCE_OPEN.equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A maintenance request can only be cancelled while it is open");
        }
        request.setStatus(Constants.MAINTENANCE_CANCELLED);
        return toResponse(maintenanceRequestRepository.save(request));
    }

    @Transactional
    public MaintenanceResponse updateStatus(Long requestId, String status, Long ownerId) {
        User owner = requireUser(ownerId);
        requireRole(owner, Constants.ROLE_OWNER);
        MaintenanceRequest request = maintenanceRequestRepository.findByIdAndProperty_OwnerId(requestId, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));

        String nextStatus = status == null ? "" : status.trim().toUpperCase();
        boolean validTransition = (Constants.MAINTENANCE_OPEN.equals(request.getStatus())
                && (Constants.MAINTENANCE_IN_PROGRESS.equals(nextStatus)
                        || Constants.MAINTENANCE_RESOLVED.equals(nextStatus)))
                || (Constants.MAINTENANCE_IN_PROGRESS.equals(request.getStatus())
                        && Constants.MAINTENANCE_RESOLVED.equals(nextStatus));
        if (!validTransition) {
            throw new IllegalArgumentException("Invalid maintenance status transition: "
                    + request.getStatus() + " -> " + nextStatus);
        }

        request.setStatus(nextStatus);
        request.setResolvedAt(Constants.MAINTENANCE_RESOLVED.equals(nextStatus) ? LocalDateTime.now() : null);
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
        comment.setCreatedAt(LocalDateTime.now());
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
            requireRole(owner, Constants.ROLE_OWNER);
            return maintenanceRequestRepository.findByIdAndProperty_OwnerId(requestId, ownerId)
                    .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));
        }

        User tenant = requireUser(tenantId);
        requireRole(tenant, Constants.ROLE_TENANT);
        return maintenanceRequestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Maintenance request not found: " + requestId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    private void requireRole(User user, String requiredRole) {
        if (!requiredRole.equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This action is not allowed for this user role");
        }
    }

    private void validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private void validatePriority(String priority) {
        if (priority == null || !("LOW".equalsIgnoreCase(priority.trim())
                || "MEDIUM".equalsIgnoreCase(priority.trim())
                || "HIGH".equalsIgnoreCase(priority.trim()))) {
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
        response.setPriority(request.getPriority());
        response.setStatus(request.getStatus());
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
        response.setComment(comment.getComment());
        response.setCreatedAt(comment.getCreatedAt());
        return response;

    }

}