package Tenant.Owner.Management.TOM.Controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import Tenant.Owner.Management.TOM.DTO.NoticeResponse;
import Tenant.Owner.Management.TOM.Service.NoticeService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;

@RestController
@RequestMapping("/api/me")
public class MeNoticeController {
    private static final int MAX_PAGE_SIZE = 5;
    private final NoticeService noticeService;

    public MeNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/notices")
    public List<NoticeResponse> getMyNotices(@AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
        return noticeService.getNotices(null, tenantId);
    }

    @GetMapping("/notices/page")
    public Page<NoticeResponse> getMyNoticesPage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long propertyId) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be positive");
        }
        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(page, cappedSize, sort);
        Long tenantId = JwtPrincipal.userId(jwt);
        return noticeService.getNoticesPage(null, tenantId, pageable, search, propertyId);
    }
}