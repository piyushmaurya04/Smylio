package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
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
import Tenant.Owner.Management.TOM.DTO.LoginRequest;
import Tenant.Owner.Management.TOM.DTO.RegisterRequest;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Exceptions.RateLimitExceededException;
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
        verify(userRepository, times(1)).save(any(User.class));
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
}

