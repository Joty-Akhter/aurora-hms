package com.easyops.hospitalcard.domain.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardAccountRepository extends JpaRepository<CardAccount, UUID> {

    Optional<CardAccount> findByCardId(UUID cardId);

    /**
     * Accounts with current_balance > 0 (prepaid liabilities), optionally filtered by card product and owner type.
     */
    @Query(value = "SELECT a.* FROM hospital_card.card_accounts a INNER JOIN hospital_card.cards c ON c.id = a.card_id " +
        "WHERE a.current_balance > 0 " +
        "AND (CAST(:productId AS uuid) IS NULL OR c.card_product_id = CAST(:productId AS uuid)) " +
        "AND (:ownerType IS NULL OR :ownerType = '' OR c.owner_type = :ownerType)",
        nativeQuery = true)
    List<CardAccount> findLiabilities(@Param("productId") UUID productId, @Param("ownerType") String ownerType);
}
