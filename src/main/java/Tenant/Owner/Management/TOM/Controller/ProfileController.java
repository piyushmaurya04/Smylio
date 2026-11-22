package Tenant.Owner.Management.TOM.Controller;

import java.time.Duration;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Tenant.Owner.Management.TOM.DTO.AuthResponse;
import Tenant.Owner.Management.TOM.DTO.ChangePasswordRequest;
import Tenant.Owner.Management.TOM.DTO.ProfileUpdateRequest;
import Tenant.Owner.Management.TOM.DTO.SwitchRoleRequest;
import Tenant.Owner.Management.TOM.DTO.UserResponse;
import Tenant.Owner.Management.TOM.DTO.WorkspaceBadgesResponse;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;
import Tenant.Owner.Management.TOM.Security.RateLimitService;
import Tenant.Owner.Management.TOM.Service.DashboardService;
import Tenant.Owner.Management.TOM.Service.RefreshTokenService;
import Tenant.Owner.Management.TOM.Service.UserService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    static final String REFRESH_COOKIE_NAME = "smylio_refresh";

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;
    private final DashboardService dashboardService;
    private final String cookieSecureMode;

    public ProfileController(UserService userService,
            RefreshTokenService refreshTokenService,
            RateLimitService rateLimitService,
            DashboardService dashboardService,
            @Value("${app.security.refresh.cookie-secure:auto}") String cookieSecureMode) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimitService = rateLimitService;
        this.dashboardService = dashboardService;
        this.cookieSecureMode = cookieSecureMode;
    }

    @GetMapping
    public UserResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(JwtPrincipal.userId(jwt));
    }

    @PutMapping
    public UserResponse updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return userService.updateProfile(JwtPrincipal.userId(jwt), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        Long userId = JwtPrincipal.userId(jwt);
        rateLimitService.consumePasswordChange(userId);
        userService.changePassword(userId, request);

        // Issue a fresh replacement refresh cookie for the current session
        String newRefreshToken = refreshTokenService.issueForUser(userId);
        ResponseCookie refreshCookie = buildRefreshCookie(newRefreshToken,
                Duration.ofDays(refreshTokenService.getExpirationDays()), httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/roles/activate")
    public AuthResponse activateRole(
            @Valid @RequestBody SwitchRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtPrincipal.userId(jwt);
        rateLimitService.consumeActivateRole(userId);
        return userService.activateRole(userId, request.getRole());
    }

    @PostMapping("/switch-role")
    public AuthResponse switchRole(
            @Valid @RequestBody SwitchRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtPrincipal.userId(jwt);
        rateLimitService.consumeSwitchRole(userId);
        return userService.switchRole(userId, request.getRole());
    }

    @GetMapping("/badges")
    public WorkspaceBadgesResponse getBadges(@AuthenticationPrincipal Jwt jwt) {
        return dashboardService.getWorkspaceBadges(JwtPrincipal.userId(jwt));
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge, HttpServletRequest httpRequest) {
        boolean secure = "true".equalsIgnoreCase(cookieSecureMode)
                || ("auto".equalsIgnoreCase(cookieSecureMode) && httpRequest.isSecure());
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
