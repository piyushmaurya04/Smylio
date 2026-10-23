package Tenant.Owner.Management.TOM.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import Tenant.Owner.Management.TOM.DTO.LeaseResponse;
import Tenant.Owner.Management.TOM.Service.LeaseService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;

@RestController
@RequestMapping("/api/me")
public class MeLeaseController {
    private final LeaseService leaseService;

    public MeLeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @GetMapping("/lease")
    public List<LeaseResponse> getMyActiveLeases(@AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
        return leaseService.getMyActiveLeases(tenantId);
    }
}