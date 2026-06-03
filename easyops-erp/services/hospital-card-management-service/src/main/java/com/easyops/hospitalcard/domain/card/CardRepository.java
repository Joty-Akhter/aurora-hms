package com.easyops.hospitalcard.domain.card;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByCardNumber(String cardNumber);

    Page<Card> findByOwnerTypeAndOwnerReferenceId(String ownerType, String ownerReferenceId, Pageable pageable);

    List<Card> findByOwnerTypeAndOwnerReferenceIdAndCardProductId(String ownerType, String ownerReferenceId, UUID cardProductId);

    Page<Card> findByCorporateId(UUID corporateId, Pageable pageable);

    List<Card> findByLimitProfileId(UUID limitProfileId);

    List<Card> findByCardProductIdAndLimitProfileIdIsNull(UUID cardProductId);
}
