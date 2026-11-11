package Tenant.Owner.Management.TOM.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Tenant.Owner.Management.TOM.Entity.RefreshToken;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Exceptions.InvalidRefreshTokenException;
import Tenant.Owner.Management.TOM.Repository.RefreshTokenRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@Service
public class RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final long expirationDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            Clock clock,
            @Value("${app.security.refresh.expiration-days:14}") long expirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.expirationDays = expirationDays;
    }

    @Transactional
    public String issueForUser(Long userId) {
        User user = userRepository.getReferenceById(userId);
        return saveToken(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedSession rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }

        String hash = sha256Hex(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHashWithUser(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        // Extract initialized User before modifying/clearing the persistence context
        User user = existing.getUser();
        Long userId = user.getId();

        LocalDateTime now = LocalDateTime.now(clock);
        if (existing.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(existing.getFamilyId(), now);
            logger.warn("Refresh token reuse detected; revoked familyId={}", existing.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token reuse detected; session revoked");
        }

        if (!existing.getExpiresAt().isAfter(now)) {
            refreshTokenRepository.markRevokedIfActive(existing.getId(), now);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        int consumed = refreshTokenRepository.markRevokedIfActive(existing.getId(), now);
        if (consumed == 0) {
            refreshTokenRepository.revokeFamily(existing.getFamilyId(), now);
            logger.warn("Concurrent refresh token replay detected; revoked familyId={}", existing.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token reuse detected; session revoked");
        }

        String replacementRawToken = saveToken(userRepository.getReferenceById(userId), existing.getFamilyId());
        return new RotatedSession(user, replacementRawToken);
    }

    @Transactional
    public void revokeByRawToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), LocalDateTime.now(clock)));
    }

    public long getExpirationDays() {
        return expirationDays;
    }

    private String saveToken(User user, UUID familyId) {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(sha256Hex(rawToken));
        token.setFamilyId(familyId);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusDays(expirationDays));
        refreshTokenRepository.save(token);
        return rawToken;
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    public record RotatedSession(User user, String refreshToken) {
    }
}
