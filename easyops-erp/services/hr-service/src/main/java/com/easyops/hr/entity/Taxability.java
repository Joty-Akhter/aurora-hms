package com.easyops.hr.entity;

/**
 * SC-21: Taxability for income tax computation.
 * Taxable, Exempt, or PartiallyTaxable (with optional rule or cap).
 */
public enum Taxability {
    TAXABLE,
    EXEMPT,
    PARTIALLY_TAXABLE
}
