package com.easyops.hospitalcorporatediscount.domain.corporatecard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CorporateCardRepository extends JpaRepository<CorporateCard, UUID>, JpaSpecificationExecutor<CorporateCard> {
    Optional<CorporateCard> findByCardId(UUID cardId);

    boolean existsByCorporateClientIdAndHolderIdentifierIgnoreCaseAndCardTypeAndStatusIn(
            UUID corporateClientId,
            String holderIdentifier,
            String cardType,
            Iterable<String> statuses
    );

    Optional<CorporateCard> findByCardNumber(String cardNumber);
}
