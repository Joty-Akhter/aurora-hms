package com.easyops.organization.repository;

import com.easyops.organization.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Invitation Repository
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    List<Invitation> findByOrganizationId(UUID organizationId);

    List<Invitation> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByEmail(String email);

    List<Invitation> findByEmailAndStatus(String email, String status);

    void deleteByExpiresAtBefore(ZonedDateTime dateTime);

    boolean existsByEmailAndOrganizationIdAndStatus(String email, UUID organizationId, String status);
}

