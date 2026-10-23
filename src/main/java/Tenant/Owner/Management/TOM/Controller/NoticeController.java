package Tenant.Owner.Management.TOM.Controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import Tenant.Owner.Management.TOM.DTO.NoticeCreateRequest;
import Tenant.Owner.Management.TOM.DTO.NoticeResponse;
import Tenant.Owner.Management.TOM.Service.NoticeService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private static final int MAX_PAGE_SIZE = 5;
    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PostMapping
    public ResponseEntity<NoticeResponse> create(
            @RequestBody NoticeCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(noticeService.create(request, ownerId));
    }

    @GetMapping
    public List<NoticeResponse> getNotices(
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        return noticeService.getNotices(ownerId, tenantId);
    }

    @PutMapping("/{id}")
    public NoticeResponse update(
            @PathVariable Long id,
            @RequestBody NoticeCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return noticeService.update(id, request, ownerId);
    }

    @GetMapping("/page")
    public Page<NoticeResponse> getNoticesPage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long propertyId) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be positive");
        }
        Pageable pageable = pageable(page, size);
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        return noticeService.getNoticesPage(ownerId, tenantId, pageable, search, propertyId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        noticeService.delete(id, ownerId);
        return ResponseEntity.noContent().build();
    }

    private Pageable pageable(int page, int size) {
        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return PageRequest.of(page, cappedSize, sort);
    }
}