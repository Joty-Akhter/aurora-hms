package com.easyops.auth.repository;

import com.easyops.auth.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    @Modifying
    @Query("UPDATE ApiKey k SET k.lastUsedAt = :ts, k.updatedAt = :ts WHERE k.id = :id")
    void updateLastUsed(@Param("id") UUID id, @Param("ts") OffsetDateTime ts);
}
