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

    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_TRACKED_KEYS;
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

    private Bucket newBucket(long capacity, Duration refillWindow) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillWindow)
                        .build())
                .build();
    }
}

