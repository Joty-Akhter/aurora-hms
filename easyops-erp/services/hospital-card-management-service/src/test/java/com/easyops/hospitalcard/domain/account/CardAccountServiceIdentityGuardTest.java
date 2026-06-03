package com.easyops.hospitalcard.domain.account;

import com.easyops.hospitalcard.domain.card.Card;
import com.easyops.hospitalcard.domain.card.CardRepository;
import com.easyops.hospitalcard.domain.metrics.CardMetrics;
import com.easyops.hospitalcard.domain.product.CardProduct;
import com.easyops.hospitalcard.domain.product.CardProductRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CardAccountServiceIdentityGuardTest {

    @Test
    void getBalance_rejectsIdentityOnlyProduct() {
        CardAccountRepository cardAccountRepository = mock(CardAccountRepository.class);
        CardTransactionRepository cardTransactionRepository = mock(CardTransactionRepository.class);
        CardRepository cardRepository = mock(CardRepository.class);
        CardProductRepository cardProductRepository = mock(CardProductRepository.class);
        CardMetrics cardMetrics = new CardMetrics(new SimpleMeterRegistry());
        CardAccountService cardAccountService = new CardAccountService(
                cardAccountRepository,
                cardTransactionRepository,
                cardRepository,
                cardProductRepository,
                null,
                cardMetrics
        );

        UUID cardId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Card card = new Card();
        card.setId(cardId);
        card.setCardProductId(productId);

        CardProduct product = new CardProduct();
        product.setId(productId);
        product.setCode("STAFF_IDENTITY");

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardProductRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cardAccountService.getBalance(cardId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wallet operations are not allowed for identity-only card product");
    }
}
