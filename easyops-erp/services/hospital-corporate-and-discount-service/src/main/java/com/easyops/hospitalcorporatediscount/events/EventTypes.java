package com.easyops.hospitalcorporatediscount.events;

/**
 * Event type names for corporate-discount domain events.
 * Payload shapes are documented in EVENTS.md.
 */
public final class EventTypes {

    private EventTypes() {}

    public static final String CORPORATE_CREATED = "corporate.created";
    public static final String CORPORATE_UPDATED = "corporate.updated";
    public static final String CORPORATE_DEACTIVATED = "corporate.deactivated";

    public static final String CONTRACT_CREATED = "contract.created";
    public static final String CONTRACT_UPDATED = "contract.updated";
    public static final String CONTRACT_EXPIRED = "contract.expired";

    public static final String COVERAGE_RULE_CREATED = "coverage-rule.created";
    public static final String COVERAGE_RULE_UPDATED = "coverage-rule.updated";
    public static final String COVERAGE_RULE_DELETED = "coverage-rule.deleted";

    public static final String DISCOUNT_SCHEME_CREATED = "discount-scheme.created";
    public static final String DISCOUNT_SCHEME_UPDATED = "discount-scheme.updated";
    public static final String DISCOUNT_SCHEME_DEACTIVATED = "discount-scheme.deactivated";

    public static final String DISCOUNT_DECISION_CREATED = "discount-decision.created";
}
