package com.easyops.hospitalcorporatediscount.domain.corporatecard;

import com.easyops.hospitalcorporatediscount.api.dto.CorporateCardValidationResponse;
import com.easyops.hospitalcorporatediscount.domain.contract.CorporateContractRepository;
import com.easyops.hospitalcorporatediscount.domain.corporate.CorporateClient;
import com.easyops.hospitalcorporatediscount.domain.corporate.CorporateClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorporateCardServiceTest {

    @Mock
    private CorporateCardRepository corporateCardRepository;
    @Mock
    private CorporateClientRepository corporateClientRepository;
    @Mock
    private CorporateContractRepository corporateContractRepository;
    private CorporateCardService service;

    @BeforeEach
    void setUp() {
        service = new CorporateCardService(corporateCardRepository, corporateClientRepository, corporateContractRepository, noopRegistryClient());
    }

    @Test
    void validateForBilling_invalidWhenCorporatePolicyNotActive() {
        UUID cardId = UUID.randomUUID();
        String cardNumber = "CORP-0001";

        CorporateCard card = new CorporateCard();
        card.setId(UUID.randomUUID());
        card.setCorporateClientId(UUID.randomUUID());
        card.setCardId(cardId);
        card.setCardNumber(cardNumber);
        card.setStatus("ACTIVE");
        card.setValidFrom(LocalDate.now().minusDays(10));
        card.setValidTo(LocalDate.now().plusDays(10));

        CorporateClient client = new CorporateClient();
        client.setId(card.getCorporateClientId());
        client.setStatus("INACTIVE");

        when(corporateCardRepository.findByCardNumber(cardNumber)).thenReturn(Optional.of(card));
        when(corporateClientRepository.findById(card.getCorporateClientId())).thenReturn(Optional.of(client));
        service = new CorporateCardService(
                corporateCardRepository,
                corporateClientRepository,
                corporateContractRepository,
                registryClientWithSearch(new HospitalCardRegistryClient.RegistryCardResult(cardId, cardNumber, "ACTIVE"))
        );

        CorporateCardValidationResponse response = service.validateForBilling(cardNumber, UUID.randomUUID(), UUID.randomUUID());
        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Corporate policy is not active for this card");
    }

    @Test
    void reprint_rejectsNonActiveCard() {
        CorporateCard card = new CorporateCard();
        card.setId(UUID.randomUUID());
        card.setStatus("BLOCKED");
        when(corporateCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.reprint(card.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void block_setsCardToBlockedWhenRegistrySyncSucceeds() {
        UUID corporateCardId = UUID.randomUUID();
        UUID registryCardId = UUID.randomUUID();
        CorporateCard card = new CorporateCard();
        card.setId(corporateCardId);
        card.setCardId(registryCardId);
        card.setStatus("ACTIVE");

        when(corporateCardRepository.findById(corporateCardId)).thenReturn(Optional.of(card));
        service = new CorporateCardService(
                corporateCardRepository,
                corporateClientRepository,
                corporateContractRepository,
                registryClientWithUpdate(new HospitalCardRegistryClient.RegistryCardResult(registryCardId, "CORP-0002", "BLOCKED"))
        );
        when(corporateCardRepository.save(any(CorporateCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.block(corporateCardId, "Lost", UUID.randomUUID(), UUID.randomUUID());
        assertThat(response.getStatus()).isEqualTo("BLOCKED");
    }

    private static HospitalCardRegistryClient noopRegistryClient() {
        return new HospitalCardRegistryClient(new RestTemplate(), "http://noop") {};
    }

    private static HospitalCardRegistryClient registryClientWithSearch(HospitalCardRegistryClient.RegistryCardResult result) {
        return new HospitalCardRegistryClient(new RestTemplate(), "http://noop") {
            @Override
            public RegistryCardResult findByCardNumber(UUID actorUserId, UUID organizationId, String cardNumber) {
                return result;
            }
        };
    }

    private static HospitalCardRegistryClient registryClientWithUpdate(HospitalCardRegistryClient.RegistryCardResult result) {
        return new HospitalCardRegistryClient(new RestTemplate(), "http://noop") {
            @Override
            public RegistryCardResult updateStatus(UUID actorUserId, UUID organizationId, UUID cardId, String status, String reason) {
                return result;
            }
        };
    }
}
