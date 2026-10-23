package Tenant.Owner.Management.TOM.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import Tenant.Owner.Management.TOM.DTO.TenantDetailsResponse;
import Tenant.Owner.Management.TOM.DTO.UserResponse;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;
import Tenant.Owner.Management.TOM.Service.TenantDetailsService;
import Tenant.Owner.Management.TOM.Service.TenantService;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService tenantService;
    private final TenantDetailsService tenantDetailsService;

    public TenantController(TenantService tenantService, TenantDetailsService tenantDetailsService) {
        this.tenantService = tenantService;
        this.tenantDetailsService = tenantDetailsService;
    }

    @GetMapping("/search")
    public UserResponse searchByTenantCode(@RequestParam String code) {
        return tenantService.findByTenantCode(code);
    }

    @GetMapping("/{tenantId}")
    public TenantDetailsResponse getTenantDetails(
            @PathVariable Long tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        return tenantDetailsService.getTenantDetails(tenantId, JwtPrincipal.userId(jwt));
    }
}