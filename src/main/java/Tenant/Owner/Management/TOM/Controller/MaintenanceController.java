package Tenant.Owner.Management.TOM.Controller;

import java.util.List;
import java.util.Locale;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.Constants.MaintenanceStatus;
import Tenant.Owner.Management.TOM.DTO.CommentResponse;
import Tenant.Owner.Management.TOM.DTO.MaintenanceCommentCreate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceRequestCreate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceRequestUpdate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceResponse;
import Tenant.Owner.Management.TOM.DTO.MaintenanceStatusUpdate;
import Tenant.Owner.Management.TOM.Service.MaintenanceService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;
import Tenant.Owner.Management.TOM.Security.RateLimitService;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);
    private static final int MAX_PAGE_SIZE = 5;
    private final MaintenanceService maintenanceService;
    private final RateLimitService rateLimitService;

    public MaintenanceController(MaintenanceService maintenanceService, RateLimitService rateLimitService) {
        this.maintenanceService = maintenanceService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/page")
    public Page<MaintenanceResponse> getRequestsPage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be positive");
        }

        MaintenanceStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = MaintenanceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid maintenance status: " + status);
            }
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSortProperty = switch (sortBy == null ? "" : sortBy.trim().toLowerCase(Locale.ROOT)) {
            case "status" -> "status";
            case "priority" -> "priority";
            case "title" -> "title";
            default -> "createdAt";
        };
        Sort sort = Sort.by(new Sort.Order(direction, safeSortProperty), new Sort.Order(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), sort);

        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        logger.info("Maintenance list requested: actorRole={} actorId={} page={} size={} propertyId={} status={} sortBy={} sortDir={}",
                jwt.getClaimAsString("role"), jwt.getSubject(), page, pageable.getPageSize(), propertyId, statusEnum, safeSortProperty, direction);
        Page<MaintenanceResponse> result = maintenanceService.getRequestsPage(
                ownerId, tenantId, pageable, search, propertyId, statusEnum);
        logger.info("Maintenance list completed: actorRole={} actorId={} page={} totalElements={}",
                jwt.getClaimAsString("role"), jwt.getSubject(), page, result.getTotalElements());
        return result;
    }

    @GetMapping("/{id}")
    public MaintenanceResponse getRequest(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        return maintenanceService.getRequest(id, ownerId, tenantId);
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> raiseRequest(
            @Valid @RequestBody MaintenanceRequestCreate request,
            @AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
        rateLimitService.consumeMaintenanceRequest(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.raiseRequest(request, tenantId));
    }

    @GetMapping("/property/{propertyId}")
    public List<MaintenanceResponse> getPropertyRequests(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        return maintenanceService.getPropertyRequests(propertyId, ownerId, tenantId);
    }

    @PutMapping("/{id}/status")
    public MaintenanceResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceStatusUpdate request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return maintenanceService.updateStatus(id, request.getStatus(), ownerId);
    }

    @PutMapping("/{id}")
    public MaintenanceResponse updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRequestUpdate request,
            @AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
        return maintenanceService.updateRequest(id, request, tenantId);
    }

    @PutMapping("/{id}/cancel")
    public MaintenanceResponse cancelRequest(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
        return maintenanceService.cancelRequest(id, tenantId);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceCommentCreate request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        Long authorId = ownerId != null ? ownerId : tenantId;
        if (authorId != null) {
            rateLimitService.consumeCommentCreation(authorId);
        }
        CommentResponse response = maintenanceService.addComment(id, request.getComment(), ownerId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        return maintenanceService.getComments(id, ownerId, tenantId);
    }
}