package com.easyops.hospitalbilling.domain.invoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LegacyInvoiceNotificationFallbackService {

    private static final Logger log = LoggerFactory.getLogger(LegacyInvoiceNotificationFallbackService.class);

    public void notifyInvoiceCreated(Invoice invoice) {
        // Temporary compatibility path for Phase 4 rollout.
        // TODO(phase4-cleanup, remove-by: 2026-06-30): Remove legacy fallback after communication-service cutover.
        log.info("Legacy invoice notification fallback invoked for invoice={}", invoice.getId());
    }
}
