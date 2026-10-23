package Tenant.Owner.Management.TOM.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import Tenant.Owner.Management.TOM.DTO.OwnerDashboardResponse;
import Tenant.Owner.Management.TOM.Service.DashboardService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/owner")
    public OwnerDashboardResponse getOwnerDashboard(@AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return dashboardService.getOwnerDashboard(ownerId);
    }
}