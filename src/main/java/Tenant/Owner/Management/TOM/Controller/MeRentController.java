package Tenant.Owner.Management.TOM.Controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Tenant.Owner.Management.TOM.DTO.TenantRentsResponse;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;
import Tenant.Owner.Management.TOM.Service.RentService;

@RestController
@RequestMapping("/api/me")
public class MeRentController {
    private final RentService rentService;

    public MeRentController(RentService rentService) {
        this.rentService = rentService;
    }

    @GetMapping("/rents")
    public TenantRentsResponse getMyRents(@AuthenticationPrincipal Jwt jwt) {
        return rentService.getMyRents(JwtPrincipal.userId(jwt));
    }
}