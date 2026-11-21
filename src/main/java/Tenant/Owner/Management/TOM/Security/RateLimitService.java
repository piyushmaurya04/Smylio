package Tenant.Owner.Management.TOM.Security;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import Tenant.Owner.Management.TOM.Exceptions.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Service
public class RateLimitService {
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final int TENANT_SEARCH_PER_MINUTE = 20;
    private static final int LOGIN_PER_IP_PER_MINUTE = 20;
    private static final int LOGIN_PER_EMAIL_PER_MINUTE = 10;
    private static final int REGISTER_PER_IP_PER_MINUTE = 30;
    private static final int REGISTER_PER_EMAIL_PER_MINUTE = 5;
    private static final int PASSWORD_CHANGE_PER_WINDOW = 5;
    private static final int COMMENT_CREATION_PER_MINUTE = 30;
    private static final int MAINTENANCE_REQUEST_PER_WINDOW = 15;
    private static final int NOTICE_CREATION_PER_WINDOW = 20;
    private static final int SWITCH_ROLE_PER_MINUTE = 30;
    private static final int ACTIVATE_ROLE_PER_HOUR = 5;

    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    if (size() <= MAX_TRACKED_KEYS) {
                        return false;
                    }
                    // Prevent attackers from flushing out active penalty buckets with dummy keys
                    return eldest.getValue().getAvailableTokens() > 0 || size() > MAX_TRACKED_KEYS * 2;
                }
            });

    public void consumeTenantSearch(Long ownerId) {
        Bucket bucket = buckets.computeIfAbsent(
                "tenant-search:" + ownerId,
                ignored -> newBucket(TENANT_SEARCH_PER_MINUTE, Duration.ofMinutes(1)));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many tenant searches. Please wait a minute and try again.");
        }
    }

    public void consumeLoginAttempt(String clientIp, String normalizedEmail) {
        String ipKey = "login-ip:" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp);
        Bucket ipBucket = buckets.computeIfAbsent(
                ipKey,
                ignored -> newBucket(LOGIN_PER_IP_PER_MINUTE, Duration.ofMinutes(1)));
        if (!ipBucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many login attempts from this address. Please wait and try again.");
        }

        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            Bucket emailBucket = buckets.computeIfAbsent(
                    "login-email:" + normalizedEmail,
                    ignored -> newBucket(LOGIN_PER_EMAIL_PER_MINUTE, Duration.ofMinutes(1)));
            if (!emailBucket.tryConsume(1)) {
                throw new RateLimitExceededException("Too many login attempts for this email. Please wait and try again.");
            }
        }
    }

    public void consumeRegisterAttempt(String clientIp, String normalizedEmail) {
        String ipKey = "register-ip:" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp);
        Bucket ipBucket = buckets.computeIfAbsent(
                ipKey,
                ignored -> newBucket(REGISTER_PER_IP_PER_MINUTE, Duration.ofMinutes(1)));
        if (!ipBucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many registration attempts from this address. Please wait and try again.");
        }

        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            Bucket emailBucket = buckets.computeIfAbsent(
                    "register-email:" + normalizedEmail,
                    ignored -> newBucket(REGISTER_PER_EMAIL_PER_MINUTE, Duration.ofMinutes(1)));
            if (!emailBucket.tryConsume(1)) {
                throw new RateLimitExceededException("Too many registration attempts for this email. Please wait and try again.");
            }
        }
    }

    public void consumePasswordChange(Long userId) {
        Bucket bucket = buckets.computeIfAbsent(
                "password-change:" + userId,
                ignored -> newBucket(PASSWORD_CHANGE_PER_WINDOW, Duration.ofMinutes(15)));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many password change attempts. Please wait 15 minutes and try again.");
        }
    }

    public void consumeCommentCreation(Long userId) {
        Bucket bucket = buckets.computeIfAbsent(
                "comment-create:" + userId,
                ignored -> newBucket(COMMENT_CREATION_PER_MINUTE, Duration.ofMinutes(1)));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many comments submitted. Please wait a minute and try again.");
        }
    }

    public void consumeMaintenanceRequest(Long tenantId) {
        Bucket bucket = buckets.computeIfAbsent(
                "maintenance-create:" + tenantId,
                ignored -> newBucket(MAINTENANCE_REQUEST_PER_WINDOW, Duration.ofMinutes(10)));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many maintenance requests raised. Please wait a few minutes and try again.");
        }
    }

    public void consumeNoticeCreation(Long ownerId) {
        Bucket bucket = buckets.computeIfAbsent(
                "notice-create:" + ownerId,
                ignored -> newBucket(NOTICE_CREATION_PER_WINDOW, Duration.ofMinutes(10)));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many notices published. Please wait a few minutes and try again.");
        }
    }

    public void consumeSwitchRole(Long userId) {
        Bucket bucket = buckets.computeIfAbsent(
                "switch-role:" + userId,
                ignored -> newBucket(SWITCH_ROLE_PER_MINUTE, Duration.ofMinutes(1)));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many workspace switch requests. Please wait a minute and try again.");
        }
    }

    public void consumeActivateRole(Long userId) {
        Bucket bucket = buckets.computeIfAbsent(
                "activate-role:" + userId,
                ignored -> newBucket(ACTIVATE_ROLE_PER_HOUR, Duration.ofHours(1)));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many role activation attempts. Please wait an hour and try again.");
        }
    }

    private Bucket newBucket(long capacity, Duration refillWindow) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillWindow)
                        .build())
                .build();
    }
}

