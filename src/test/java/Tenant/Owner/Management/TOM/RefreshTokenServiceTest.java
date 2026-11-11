package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.Entity.RefreshToken;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Exceptions.InvalidRefreshTokenException;
import Tenant.Owner.Management.TOM.Repository.RefreshTokenRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-15T04:30:00Z"), IST);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, userRepository, FIXED_CLOCK, 14);
    }

    @Test
    void issueStoresSha256HashNotRawToken() {
        User user = user(7L);
        when(userRepository.getReferenceById(7L)).thenReturn(user);

        String rawToken = refreshTokenService.issueForUser(7L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertNotNull(rawToken);
        assertNotEquals(rawToken, saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length());
        assertEquals(LocalDateTime.now(FIXED_CLOCK).plusDays(14), saved.getExpiresAt());
    }

    @Test
    void rotateConsumesTokenAndPreservesFamilyId() {
        User user = user(7L);
        UUID familyId = UUID.randomUUID();
        RefreshToken active = new RefreshToken();
        active.setId(10L);
        active.setUser(user);
        active.setFamilyId(familyId);
        active.setCreatedAt(LocalDateTime.now(FIXED_CLOCK).minusDays(1));
        active.setExpiresAt(LocalDateTime.now(FIXED_CLOCK).plusDays(13));

        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.of(active));
        when(refreshTokenRepository.markRevokedIfActive(10L, LocalDateTime.now(FIXED_CLOCK))).thenReturn(1);
        when(userRepository.getReferenceById(7L)).thenReturn(user);

        var rotated = refreshTokenService.rotate("raw-cookie-token");

        assertEquals(user, rotated.user());
        assertNotNull(rotated.refreshToken());
        assertNotEquals("raw-cookie-token", rotated.refreshToken());
        ArgumentCaptor<RefreshToken> savedCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(savedCaptor.capture());
        assertEquals(familyId, savedCaptor.getValue().getFamilyId());
    }

    @Test
    void reuseOfRevokedTokenRevokesEntireFamily() {
        User user = user(7L);
        UUID familyId = UUID.randomUUID();
        RefreshToken revoked = new RefreshToken();
        revoked.setId(11L);
        revoked.setUser(user);
        revoked.setFamilyId(familyId);
        revoked.setRevokedAt(LocalDateTime.now(FIXED_CLOCK).minusMinutes(5));
        revoked.setExpiresAt(LocalDateTime.now(FIXED_CLOCK).plusDays(10));

        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.of(revoked));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate("replayed-token"));
        verify(refreshTokenRepository).revokeFamily(familyId, LocalDateTime.now(FIXED_CLOCK));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void expiredTokenIsRejectedAndRevoked() {
        User user = user(7L);
        RefreshToken expired = new RefreshToken();
        expired.setId(12L);
        expired.setUser(user);
        expired.setFamilyId(UUID.randomUUID());
        expired.setExpiresAt(LocalDateTime.now(FIXED_CLOCK).minusMinutes(1));

        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.of(expired));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate("expired-token"));
        verify(refreshTokenRepository).markRevokedIfActive(12L, LocalDateTime.now(FIXED_CLOCK));
        verify(refreshTokenRepository, never()).save(any());
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("tenant@example.test");
        user.setRole(UserRole.TENANT);
        return user;
    }
}

