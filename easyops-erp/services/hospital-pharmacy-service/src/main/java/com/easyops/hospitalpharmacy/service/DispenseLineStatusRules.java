package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.entity.DispenseLine;

import java.util.EnumSet;
import java.util.Set;

/**
 * Line-level fulfillment transitions (Phase P2 — WS-C2). Stock-issued lines progress through dispense/return;
 * unfulfilled lines are created directly as {@link DispenseLine.Status#OUT_OF_STOCK} or {@link DispenseLine.Status#REFUSED}.
 */
public final class DispenseLineStatusRules {

    private DispenseLineStatusRules() {
    }

    /** Statuses that may follow a successful stock issue on a new line. */
    public static final Set<DispenseLine.Status> POST_ISSUE_STATUSES = EnumSet.of(
            DispenseLine.Status.DISPENSED,
            DispenseLine.Status.PARTIALLY_DISPENSED,
            DispenseLine.Status.FILLED_WITH_STOCK_OVERRIDE
    );

    public static boolean isTerminal(DispenseLine.Status s) {
        return s == DispenseLine.Status.CANCELLED || s == DispenseLine.Status.RETURNED;
    }
}
