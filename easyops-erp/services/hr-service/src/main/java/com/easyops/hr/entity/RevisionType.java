package com.easyops.hr.entity;

/**
 * ES-17: Type of salary revision for assignment or component change.
 * Used with revision reason for audit and history.
 */
public enum RevisionType {
    /** Initial assignment or component add */
    INITIAL,
    /** Annual / periodic increment */
    ANNUAL_INCREMENT,
    /** Promotion (grade/band change) */
    PROMOTION,
    /** One-off adjustment */
    ADJUSTMENT,
    /** Bulk revision (e.g. X% by grade) */
    BULK_PERCENTAGE,
    /** Correction */
    CORRECTION,
    /** Other; use revision reason for detail */
    OTHER
}
