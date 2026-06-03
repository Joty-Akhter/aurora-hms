package com.easyops.hospitalcard.domain.card;

import com.easyops.hospitalcard.api.dto.IssueStaffCardRequest;
import com.easyops.hospitalcard.api.dto.ReplaceStaffCardRequest;
import com.easyops.hospitalcard.domain.account.CardAccountRepository;
import com.easyops.hospitalcard.domain.account.CardTransactionRepository;
import com.easyops.hospitalcard.domain.product.CardProduct;
import com.easyops.hospitalcard.domain.product.CardProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceStaffLifecycleTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardAccountRepository cardAccountRepository;
    @Mock
    private CardTransactionRepository cardTransactionRepository;
    @Mock
    private CardProductRepository cardProductRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CardService cardService;

    @Test
    void issueStaffIdentity_rejectsNonActiveWithoutAdminOverride() {
        IssueStaffCardRequest request = new IssueStaffCardRequest();
        request.setEmployeeId("EMP-001");
        request.setEmploymentStatus("SUSPENDED");

        assertThatThrownBy(() -> cardService.issueStaffIdentity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot issue staff card for non-active employment status");
    }

    @Test
    void issueStaffIdentity_requiresOverrideReasonWhenAdminOverrideTrue() {
        IssueStaffCardRequest request = new IssueStaffCardRequest();
        request.setEmployeeId("EMP-001");
        request.setEmploymentStatus("TERMINATED");
        request.setAdminOverride(true);

        assertThatThrownBy(() -> cardService.issueStaffIdentity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overrideReason is required");
    }

    @Test
    void replaceStaffIdentity_rejectsTerminalCard() {
        UUID cardId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Card card = new Card();
        card.setId(cardId);
        card.setOwnerType("STAFF");
        card.setCardProductId(productId);
        card.setStatus("REVOKED");

        CardProduct product = new CardProduct();
        product.setId(productId);
        product.setCode("STAFF_IDENTITY");

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardProductRepository.findById(productId)).thenReturn(Optional.of(product));

        ReplaceStaffCardRequest request = new ReplaceStaffCardRequest();
        request.setReason("LOST");
        assertThatThrownBy(() -> cardService.replaceStaffIdentity(cardId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot replace card with terminal status");
    }

    @Test
    void verifyStaffCard_requiresExactlyOneLookupParameter() {
        assertThatThrownBy(() -> cardService.verifyStaffCard(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either cardNumber or employeeId is required");

        assertThatThrownBy(() -> cardService.verifyStaffCard("CARD-1", "EMP-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provide only one lookup parameter");
    }

    @Test
    void issueStaffIdentity_rejectsDuplicateActiveCard() {
        UUID productId = UUID.randomUUID();
        CardProduct product = new CardProduct();
        product.setId(productId);
        product.setCode("STAFF_IDENTITY");

        Card activeCard = new Card();
        activeCard.setStatus("ACTIVE");

        when(cardProductRepository.findByCode("STAFF_IDENTITY")).thenReturn(Optional.of(product));
        when(cardRepository.findByOwnerTypeAndOwnerReferenceIdAndCardProductId("STAFF", "EMP-001", productId))
                .thenReturn(List.of(activeCard));

        IssueStaffCardRequest request = new IssueStaffCardRequest();
        request.setEmployeeId("EMP-001");
        request.setEmploymentStatus("ACTIVE");

        assertThatThrownBy(() -> cardService.issueStaffIdentity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IDEMPOTENCY_CONFLICT");
    }
}
