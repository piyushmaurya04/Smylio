package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.DTO.ChangePasswordRequest;
import Tenant.Owner.Management.TOM.DTO.LoginRequest;
import Tenant.Owner.Management.TOM.DTO.RegisterRequest;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Exceptions.RateLimitExceededException;
import Tenant.Owner.Management.TOM.Repository.RefreshTokenRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Security.JwtTokenService;
import Tenant.Owner.Management.TOM.Security.RateLimitService;
import Tenant.Owner.Management.TOM.Service.AuthService;
import Tenant.Owner.Management.TOM.Service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceAndAuthHardeningTest {
    private static final Pattern CROCKFORD_TENANT_CODE = Pattern.compile("^TNT-[0-9A-HJKMNP-TV-Z]{8}$");

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-03-15T04:30:00Z"), ZoneId.of("Asia/Kolkata"));

    @InjectMocks
    private UserService userService;
    @InjectMocks
    private AuthService authService;

    @Test
    void registerGeneratesNonGuessableCrockfordTenantCodeInSingleSave() {
        RegisterRequest request = new RegisterRequest();
        request.setName("  Asha Verma ");
        request.setEmail("Asha@Example.com");
        request.setPassword("StrongPass123!");
        request.setPhone("9876543210");
        request.setRole(UserRole.TENANT.name());

        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(userRepository.existsByTenantCode(anyString())).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(42L);
            return u;
        });

        var response = userService.register(request);

        assertNotNull(response.getTenantCode());
        assertTrue(CROCKFORD_TENANT_CODE.matcher(response.getTenantCode()).matches());
        assertEquals("Asha Verma", response.getName());
        assertEquals("TENANT", response.getRole());
        assertNotNull(response.getRoles());
        assertTrue(response.getRoles().contains("TENANT"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void userEntitySupportsMultiRoleWithBackwardCompatibility() {
        User user = new User();
        user.setRole(UserRole.OWNER);

        assertTrue(user.hasRole(UserRole.OWNER));
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(UserRole.OWNER));

        user.addRole(UserRole.TENANT);
        assertTrue(user.hasRole(UserRole.OWNER));
        assertTrue(user.hasRole(UserRole.TENANT));
        assertEquals(2, user.getRoles().size());
    }

    @Test
    void activateRoleEnablesTenantAndGeneratesTenantCode() {
        User owner = new User();
        owner.setId(21L);
        owner.setName("Piyush Owner");
        owner.setEmail("piyush@example.com");
        owner.setRole(UserRole.OWNER);
        owner.addRole(UserRole.OWNER);

        when(userRepository.findById(21L)).thenReturn(Optional.of(owner));
        when(userRepository.existsByTenantCode(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenService.createToken(any(User.class))).thenReturn("fake-jwt-token");
        when(jwtTokenService.getExpirationSeconds()).thenReturn(900L);

        var authResponse = userService.activateRole(21L, "TENANT");

        assertNotNull(authResponse.getAccessToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals("TENANT", authResponse.getUser().getRole());
        assertTrue(authResponse.getUser().getRoles().contains("TENANT"));
        assertTrue(authResponse.getUser().getRoles().contains("OWNER"));
        assertNotNull(authResponse.getUser().getTenantCode());
        assertTrue(CROCKFORD_TENANT_CODE.matcher(authResponse.getUser().getTenantCode()).matches());
    }

    @Test
    void switchRoleSwitchesActiveRoleBetweenEnabledRoles() {
        User dualRole = new User();
        dualRole.setId(21L);
        dualRole.setName("Piyush Dual");
        dualRole.setEmail("piyush@example.com");
        dualRole.setRole(UserRole.OWNER);
        dualRole.addRole(UserRole.OWNER);
        dualRole.addRole(UserRole.TENANT);
        dualRole.setTenantCode("TNT-12345678");

        when(userRepository.findById(21L)).thenReturn(Optional.of(dualRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenService.createToken(any(User.class))).thenReturn("fake-jwt-token-tenant");
        when(jwtTokenService.getExpirationSeconds()).thenReturn(900L);

        var authResponse = userService.switchRole(21L, "TENANT");

        assertEquals("TENANT", authResponse.getUser().getRole());
        assertTrue(authResponse.getUser().getRoles().contains("OWNER"));
        assertTrue(authResponse.getUser().getRoles().contains("TENANT"));
    }

    @Test
    void switchRoleRejectsUnactivatedRole() {
        User ownerOnly = new User();
        ownerOnly.setId(22L);
        ownerOnly.setRole(UserRole.OWNER);
        ownerOnly.addRole(UserRole.OWNER);

        when(userRepository.findById(22L)).thenReturn(Optional.of(ownerOnly));

        assertThrows(IllegalArgumentException.class, () -> userService.switchRole(22L, "TENANT"));
    }

    @Test
    void loginRunsDummyBcryptCheckWhenEmailNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@example.com");
        request.setPassword("SomePass123");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    void loginRejectsLegacyPlaintextPasswordInDatabase() {
        User legacy = new User();
        legacy.setEmail("legacy@example.com");
        legacy.setPassword("plaintext-password");
        LoginRequest request = new LoginRequest();
        request.setEmail("legacy@example.com");
        request.setPassword("plaintext-password");

        when(userRepository.findByEmail("legacy@example.com")).thenReturn(Optional.of(legacy));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(userRepository, times(0)).save(any());
    }

    @Test
    void tenantSearchRateLimiterBlocksExcessiveBurst() {
        RateLimitService rateLimitService = new RateLimitService();
        for (int i = 0; i < 20; i++) {
            rateLimitService.consumeTenantSearch(1L);
        }
        assertThrows(RateLimitExceededException.class, () -> rateLimitService.consumeTenantSearch(1L));
    }

    @Test
    void registerRateLimiterBlocksExcessiveBurstByIp() {
        RateLimitService rateLimitService = new RateLimitService();
        for (int i = 0; i < 30; i++) {
            rateLimitService.consumeRegisterAttempt("192.168.1.50", "user" + i + "@example.com");
        }
        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.consumeRegisterAttempt("192.168.1.50", "overflow@example.com"));
    }

    @Test
    void registerRateLimiterBlocksExcessiveBurstByEmail() {
        RateLimitService rateLimitService = new RateLimitService();
        for (int i = 0; i < 5; i++) {
            rateLimitService.consumeRegisterAttempt("10.0.0." + i, "target@example.com");
        }
        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.consumeRegisterAttempt("10.0.0.99", "target@example.com"));
    }

    @Test
    void changePasswordSuccessfullyUpdatesPassword() {
        User user = new User();
        user.setId(10L);
        user.setPassword("old-encoded-pass");

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123", "old-encoded-pass")).thenReturn(true);
        when(passwordEncoder.matches("NewPass12345", "old-encoded-pass")).thenReturn(false);
        when(passwordEncoder.encode("NewPass12345")).thenReturn("new-encoded-pass");

        userService.changePassword(10L, new ChangePasswordRequest("CurrentPass123", "NewPass12345"));

        assertEquals("new-encoded-pass", user.getPassword());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(eq(10L), any(LocalDateTime.class));
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        User user = new User();
        user.setId(10L);
        user.setPassword("old-encoded-pass");

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123", "old-encoded-pass")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(10L, new ChangePasswordRequest("WrongPass123", "NewPass12345")));

        assertEquals("Current password is incorrect", ex.getMessage());
    }

    @Test
    void changePasswordRejectsSameNewPassword() {
        User user = new User();
        user.setId(10L);
        user.setPassword("old-encoded-pass");

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123", "old-encoded-pass")).thenReturn(true);
        when(passwordEncoder.matches("CurrentPass123", "old-encoded-pass")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(10L, new ChangePasswordRequest("CurrentPass123", "CurrentPass123")));

        assertEquals("New password cannot be the same as your current password", ex.getMessage());
    }

    @Test
    void changePasswordRejectsShortPassword() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(10L, new ChangePasswordRequest("CurrentPass123", "short")));

        assertEquals("New password must be at least 8 characters", ex.getMessage());
    }
}

