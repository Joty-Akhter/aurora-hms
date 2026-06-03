package com.easyops.hospitalcard.domain.report;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.account.CardTransaction;
import com.easyops.hospitalcard.domain.account.CardTransactionRepository;
import com.easyops.hospitalcard.domain.account.CardTransactionSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final CardTransactionRepository cardTransactionRepository;

    /**
     * Card-side export for reconciliation: committed transactions in date range, optional sourceSystem.
     * Use with GET /reconciliation/card-vs-billing?from=&to=&sourceSystem=
     */
    @Transactional(readOnly = true)
    public List<ReconciliationItem> getCardSideExport(
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to,
            String sourceSystem) {
        Specification<CardTransaction> spec = Specification
                .where(CardTransactionSpecifications.hasStatus("COMMITTED"))
                .and(CardTransactionSpecifications.postedFrom(from))
                .and(CardTransactionSpecifications.postedTo(to))
                .and(CardTransactionSpecifications.hasSourceSystem(trimOrNull(sourceSystem)));

        Sort sort = Sort.by(Sort.Direction.ASC, "postedAt");
        List<CardTransaction> list = cardTransactionRepository.findAll(spec, sort);
        List<ReconciliationItem> items = new ArrayList<>();
        for (CardTransaction t : list) {
            ReconciliationItem item = new ReconciliationItem();
            item.setTransactionId(t.getId());
            item.setSourceSystem(t.getSourceSystem());
            item.setExternalReferenceId(t.getExternalReferenceId());
            item.setAmount(t.getAmount());
            item.setCurrency(t.getCurrency());
            item.setPostedAt(t.getPostedAt());
            items.add(item);
        }
        return items;
    }

    /**
     * Compare Billing/Canteen entries with our card_transactions by source_system + external_reference_id.
     * Returns MATCHED, NOT_FOUND, or AMOUNT_MISMATCH per entry.
     */
    @Transactional(readOnly = true)
    public List<ReconciliationMatchResult> compare(List<ReconciliationEntryRequest> entries) {
        if (entries == null) {
            return List.of();
        }
        List<ReconciliationMatchResult> results = new ArrayList<>();
        for (ReconciliationEntryRequest req : entries) {
            ReconciliationMatchResult result = new ReconciliationMatchResult();
            result.setSourceSystem(req.getSourceSystem());
            result.setExternalReferenceId(req.getExternalReferenceId());
            result.setExpectedAmount(req.getAmount());

            var opt = cardTransactionRepository.findByExternalReferenceIdAndSourceSystem(
                    req.getExternalReferenceId(),
                    req.getSourceSystem());
            if (opt.isEmpty()) {
                result.setStatus(ReconciliationMatchResult.NOT_FOUND);
                result.setActualAmount(null);
            } else {
                CardTransaction t = opt.get();
                if (t.getStatus() != null && !"COMMITTED".equals(t.getStatus())) {
                    result.setStatus(ReconciliationMatchResult.NOT_FOUND);
                    result.setActualAmount(null);
                } else if (t.getAmount() != null && req.getAmount() != null
                        && t.getAmount().compareTo(req.getAmount()) == 0) {
                    result.setStatus(ReconciliationMatchResult.MATCHED);
                    result.setActualAmount(t.getAmount());
                } else {
                    result.setStatus(ReconciliationMatchResult.AMOUNT_MISMATCH);
                    result.setActualAmount(t.getAmount());
                }
            }
            results.add(result);
        }
        return results;
    }

    /**
     * Same as compare but returns only entries with status NOT_FOUND or AMOUNT_MISMATCH (mismatches only).
     * Used by POST /reconciliation/mismatches for a single-call "mismatches only" API.
     */
    @Transactional(readOnly = true)
    public List<ReconciliationMatchResult> mismatchesOnly(List<ReconciliationEntryRequest> entries) {
        List<ReconciliationMatchResult> all = compare(entries);
        return all.stream()
                .filter(r -> ReconciliationMatchResult.NOT_FOUND.equals(r.getStatus())
                        || ReconciliationMatchResult.AMOUNT_MISMATCH.equals(r.getStatus()))
                .toList();
    }

    private static String trimOrNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
