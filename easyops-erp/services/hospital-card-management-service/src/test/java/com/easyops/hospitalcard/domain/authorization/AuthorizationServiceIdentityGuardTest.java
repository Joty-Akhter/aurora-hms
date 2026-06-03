package com.easyops.hospitalcard.domain.authorization;

import com.easyops.hospitalcard.api.dto.AuthorizationRequest;
import com.easyops.hospitalcard.domain.account.CardAccountRepository;
import com.easyops.hospitalcard.domain.account.CardTransactionRepository;
import com.easyops.hospitalcard.domain.card.Card;
import com.easyops.hospitalcard.domain.card.CardRepository;
import com.easyops.hospitalcard.domain.metrics.CardMetrics;
import com.easyops.hospitalcard.domain.product.CardProduct;
import com.easyops.hospitalcard.domain.product.CardProductRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceIdentityGuardTest {

    @Test
    void authorize_rejectsIdentityOnlyProduct() {
        CardRepository cardRepository = mock(CardRepository.class);
        CardAccountRepository cardAccountRepository = mock(CardAccountRepository.class);
        CardTransactionRepository cardTransactionRepository = mock(CardTransactionRepository.class);
        CardMetrics cardMetrics = new CardMetrics(new SimpleMeterRegistry());
        CardProductRepository cardProductRepository = mock(CardProductRepository.class);
        AuthorizationService authorizationService = new AuthorizationService(
                cardRepository,
                cardAccountRepository,
                cardTransactionRepository,
                null,
                null,
                cardMetrics,
                cardProductRepository
        );

        UUID cardId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Card card = new Card();
        card.setId(cardId);
        card.setCardProductId(productId);
        card.setStatus("ACTIVE");

        CardProduct product = new CardProduct();
        product.setId(productId);
        product.setCode("STAFF_IDENTITY");

        when(cardRepository.findByCardNumber("CARD-001")).thenReturn(Optional.of(card));
        when(cardProductRepository.findById(productId)).thenReturn(Optional.of(product));

        AuthorizationRequest request = new AuthorizationRequest();
        request.setCardNumber("CARD-001");
        request.setAmount(new BigDecimal("10.00"));
        request.setSourceSystem("CANTEEN");

        assertThatThrownBy(() -> authorizationService.authorize(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wallet operations are not allowed for identity-only card product");
    }
}
