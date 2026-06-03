package com.easyops.hospital.service;

import com.easyops.hospital.entity.EpLookupItem;
import com.easyops.hospital.repository.EpAdviceUserUsageRepository;
import com.easyops.hospital.repository.EpLookupItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpAdviceCatalogServiceTest {

    @Mock
    private EpLookupItemRepository lookupItemRepository;

    @Mock
    private EpAdviceUserUsageRepository usageRepository;

    @InjectMocks
    private EpAdviceCatalogService epAdviceCatalogService;

    @Test
    void dismissSuggestions_deletesUsageForMatchingAdviceLine() {
        UUID userId = UUID.randomUUID();
        UUID lookupId = UUID.randomUUID();
        EpLookupItem item = EpLookupItem.builder()
                .id(lookupId)
                .category(EpAdviceCatalogService.CATEGORY_ADVICE)
                .value("Rest and fluids")
                .active(true)
                .build();

        when(lookupItemRepository.findAdviceByNormalizedPreferActive("rest and fluids"))
                .thenReturn(Optional.of(item));

        epAdviceCatalogService.dismissSuggestions(userId, List.of("  Rest   and fluids "));

        verify(usageRepository).deleteByUserIdAndLookupId(eq(userId), eq(lookupId));
    }

    @Test
    void dismissSuggestions_skipsWhenCatalogRowMissing() {
        UUID userId = UUID.randomUUID();
        when(lookupItemRepository.findAdviceByNormalizedPreferActive("unknown line"))
                .thenReturn(Optional.empty());

        epAdviceCatalogService.dismissSuggestions(userId, List.of("unknown line"));

        verify(usageRepository, never()).deleteByUserIdAndLookupId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
