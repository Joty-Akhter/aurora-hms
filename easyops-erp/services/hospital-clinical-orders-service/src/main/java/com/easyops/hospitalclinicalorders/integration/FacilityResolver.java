package com.easyops.hospitalclinicalorders.integration;

import java.util.UUID;

/**
 * Optional facility validation/resolution for order set creation.
 * When a bean implementing this interface is present, OrderSetService calls it to validate
 * or resolve {@code facilityId} before persisting (create and copy-from-order-set).
 * If no bean is provided, facility IDs are stored as supplied without validation.
 *
 * To enable validation (e.g. against hospital-service or a facility master API), provide
 * an implementation that throws an exception when the facility is invalid or not found.
 *
 * @see docs/FACILITY-LIS-RIS-ROUTING.md
 */
public interface FacilityResolver {

    /**
     * Validate or resolve the given facility ID. Throw if the facility is invalid or not found.
     *
     * @param facilityId optional facility ID (non-null when called)
     * @throws IllegalArgumentException when the facility does not exist or is not active
     */
    void validate(UUID facilityId);
}
