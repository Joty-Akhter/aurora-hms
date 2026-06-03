package com.easyops.hr.integration;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumes PF exit/settlement amounts for loan recovery at separation (ST-02, BR-09).
 * Default implementation returns empty until PF module exposes settlement totals.
 */
public interface PfSettlementClient {

    /**
     * Available amount from PF settlement for this employee, if known.
     */
    Optional<BigDecimal> getAvailableSettlementAmount(UUID organizationId, UUID employeeId);
}
