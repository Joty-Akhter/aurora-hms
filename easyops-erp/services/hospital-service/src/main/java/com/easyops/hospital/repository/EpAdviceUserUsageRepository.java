package com.easyops.hospital.repository;

import com.easyops.hospital.entity.EpAdviceUserUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EpAdviceUserUsageRepository extends JpaRepository<EpAdviceUserUsage, UUID> {

    @Query("""
            SELECT u FROM EpAdviceUserUsage u
            JOIN FETCH u.adviceLookupItem a
            WHERE u.userId = :userId AND a.active = true AND a.category = 'ADVICE'
            ORDER BY u.useCount DESC, u.lastUsedAt DESC
            """)
    List<EpAdviceUserUsage> findRankedForUser(@Param("userId") UUID userId);

    /**
     * Atomically insert usage (count=1) or increment count and touch {@code last_used_at}.
     * Uses PostgreSQL {@code ON CONFLICT}; {@code flushAutomatically} persists pending lookup writes first;
     * {@code clearAutomatically=false} avoids evicting the persistence context on each line in {@code recordUsage}.
     */
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(value = """
            INSERT INTO ehr.ep_advice_user_usage (id, user_id, advice_lookup_id, use_count, last_used_at)
            VALUES (gen_random_uuid(), :userId, :lookupId, 1, :now)
            ON CONFLICT (user_id, advice_lookup_id)
            DO UPDATE SET use_count = ep_advice_user_usage.use_count + 1,
                          last_used_at = EXCLUDED.last_used_at
            """, nativeQuery = true)
    void upsertIncrementUsage(@Param("userId") UUID userId, @Param("lookupId") UUID lookupId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM EpAdviceUserUsage u
            WHERE u.userId = :userId AND u.adviceLookupItem.id = :lookupId
            """)
    void deleteByUserIdAndLookupId(@Param("userId") UUID userId, @Param("lookupId") UUID lookupId);
}
