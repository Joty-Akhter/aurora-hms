package com.easyops.hr.entity;

/**
 * ES-29: Proration rule for salary components in a pay period.
 * BY_DAYS = prorate by working days in period (e.g. gross × days worked / days in period).
 * NO_PRORATION = full month amount if employed any day in period.
 * BY_HOURS = prorate by hours (when time/attendance data available).
 */
public enum ProrationRule {
    BY_DAYS,
    NO_PRORATION,
    BY_HOURS
}
