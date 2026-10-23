package Tenant.Owner.Management.TOM.Controller;

import java.util.List;

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
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.DTO.CommentResponse;
import Tenant.Owner.Management.TOM.DTO.MaintenanceCommentCreate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceRequestCreate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceRequestUpdate;
import Tenant.Owner.Management.TOM.DTO.MaintenanceResponse;
import Tenant.Owner.Management.TOM.DTO.MaintenanceStatusUpdate;
import Tenant.Owner.Management.TOM.Service.MaintenanceService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {
    private static final int MAX_PAGE_SIZE = 5;
    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/page")
    public Page<MaintenanceResponse> getRequestsPage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long propertyId) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be positive");
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        return maintenanceService.getRequestsPage(ownerId, tenantId, pageable, search, propertyId);
    }

    @GetMapping("/{id}")
    public MaintenanceResponse getRequest(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        return maintenanceService.getRequest(id, ownerId, tenantId);
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> raiseRequest(
            @RequestBody MaintenanceRequestCreate request,
            @AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
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
            @RequestBody MaintenanceStatusUpdate request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return maintenanceService.updateStatus(id, request.getStatus(), ownerId);
    }

    @PutMapping("/{id}")
    public MaintenanceResponse updateRequest(
            @PathVariable Long id,
            @RequestBody MaintenanceRequestUpdate request,
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
            @RequestBody MaintenanceCommentCreate request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
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