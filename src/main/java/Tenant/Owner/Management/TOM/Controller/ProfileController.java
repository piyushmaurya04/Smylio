package Tenant.Owner.Management.TOM.Controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Tenant.Owner.Management.TOM.DTO.ProfileUpdateRequest;
import Tenant.Owner.Management.TOM.DTO.UserResponse;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;
import Tenant.Owner.Management.TOM.Service.UserService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(JwtPrincipal.userId(jwt));
    }

    @PutMapping
    public UserResponse updateProfile(
            @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return userService.updateProfile(JwtPrincipal.userId(jwt), request);
    }
}
