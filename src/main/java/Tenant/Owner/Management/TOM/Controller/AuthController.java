package Tenant.Owner.Management.TOM.Controller;

import java.time.Duration;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Tenant.Owner.Management.TOM.DTO.RegisterRequest;
import Tenant.Owner.Management.TOM.DTO.AuthResponse;
import Tenant.Owner.Management.TOM.DTO.LoginRequest;
import Tenant.Owner.Management.TOM.DTO.UserResponse;
import Tenant.Owner.Management.TOM.Security.RateLimitService;
import Tenant.Owner.Management.TOM.Service.AuthService;
import Tenant.Owner.Management.TOM.Service.RefreshTokenService;
import Tenant.Owner.Management.TOM.Service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    static final String REFRESH_COOKIE_NAME = "smylio_refresh";

    private final UserService userService;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;
    private final String cookieSecureMode;

    public AuthController(
            UserService userService,
            AuthService authService,
            RefreshTokenService refreshTokenService,
            RateLimitService rateLimitService,
            @Value("${app.security.refresh.cookie-secure:auto}") String cookieSecureMode) {
        this.userService = userService;
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimitService = rateLimitService;
        this.cookieSecureMode = cookieSecureMode;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        rateLimitService.consumeLoginAttempt(httpRequest.getRemoteAddr(), normalizedEmail);
        AuthResponse response = authService.login(request);
        String rawRefreshToken = refreshTokenService.issueForUser(response.getUser().getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(rawRefreshToken,
                        Duration.ofDays(refreshTokenService.getExpirationDays()), httpRequest).toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken,
            HttpServletRequest httpRequest) {
        RefreshTokenService.RotatedSession rotated = refreshTokenService.rotate(rawRefreshToken);
        AuthResponse response = authService.buildAuthResponse(rotated.user());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(rotated.refreshToken(),
                        Duration.ofDays(refreshTokenService.getExpirationDays()), httpRequest).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken,
            HttpServletRequest httpRequest) {
        refreshTokenService.revokeByRawToken(rawRefreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", Duration.ZERO, httpRequest).toString())
                .build();
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
