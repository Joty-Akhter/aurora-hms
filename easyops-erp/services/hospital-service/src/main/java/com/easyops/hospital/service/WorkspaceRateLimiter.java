package com.easyops.hospital.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-user rate limiter for the EP workspace PUT endpoint.
 *
 * <p>EP-8 requirement: max 1 write per 2 seconds per user.  This prevents runaway
 * auto-save loops from saturating the database under concurrent tab usage or a
 * misbehaving client.
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>{@link ConcurrentHashMap#compute} provides per-key atomicity, making the
 *       compare-and-set safe without an explicit lock.</li>
 *   <li>The map has at most one entry per active user, so its memory footprint is
 *       proportional to the number of concurrent users, not to request volume.</li>
 *   <li>Stale entries (users who haven't written recently) are evicted lazily when
 *       the map exceeds {@link #CLEANUP_THRESHOLD} entries to prevent unbounded growth
 *       in deployments with many distinct user IDs over a long uptime.</li>
 * </ul>
 */
@Component
@Slf4j
public class WorkspaceRateLimiter {

    /** Minimum milliseconds between accepted writes for the same user. */
    static final long MIN_INTERVAL_MS = 2_000L;

    /**
     * Entries older than this are eligible for lazy eviction.
     * Set to 10× the minimum interval so no in-progress session is affected.
     */
    private static final long STALE_THRESHOLD_MS = 60_000L;

    /** Trigger a cleanup pass when the map grows beyond this many entries. */
    private static final int CLEANUP_THRESHOLD = 1_000;

    /** userId → timestamp (ms) of the last accepted write. */
    private final ConcurrentHashMap<UUID, Long> lastWriteMs = new ConcurrentHashMap<>();

    /**
     * Attempts to acquire a write permit for the given user.
     *
     * @param userId the user requesting a write
     * @return {@code true} if the write is within the rate limit and should proceed;
     *         {@code false} if the user must wait before writing again
     */
    public boolean tryAcquire(UUID userId) {
        long now = System.currentTimeMillis();

        if (lastWriteMs.size() > CLEANUP_THRESHOLD) {
            evictStale(now);
        }

        boolean[] acquired = { false };
        lastWriteMs.compute(userId, (id, lastWrite) -> {
            if (lastWrite == null || now - lastWrite >= MIN_INTERVAL_MS) {
                acquired[0] = true;
                return now;
            }
            // Rate limited: keep the existing timestamp so the window is not reset.
            return lastWrite;
        });

        if (!acquired[0]) {
            log.debug("EP workspace rate limit exceeded for user {}", userId);
        }
        return acquired[0];
    }

    /**
     * Returns how many milliseconds the user must wait before the next write will
     * be accepted, or {@code 0} if the user is not currently rate-limited.
     */
    public long retryAfterMs(UUID userId) {
        Long last = lastWriteMs.get(userId);
        if (last == null) return 0L;
        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0L, MIN_INTERVAL_MS - elapsed);
    }

    /** Removes entries whose last write was more than {@link #STALE_THRESHOLD_MS} ago. */
    private void evictStale(long now) {
        lastWriteMs.entrySet().removeIf(e -> now - e.getValue() > STALE_THRESHOLD_MS);
        log.debug("EP workspace rate-limiter eviction pass complete, {} entries remaining",
                lastWriteMs.size());
    }
}
