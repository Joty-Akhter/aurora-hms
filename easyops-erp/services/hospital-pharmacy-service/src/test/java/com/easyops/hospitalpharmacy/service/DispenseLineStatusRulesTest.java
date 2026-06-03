package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.entity.DispenseLine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispenseLineStatusRulesTest {

    @Test
    void postIssueStatuses_includeDispensedAndPartialAndOverride() {
        assertThat(DispenseLineStatusRules.POST_ISSUE_STATUSES).contains(
                DispenseLine.Status.DISPENSED,
                DispenseLine.Status.PARTIALLY_DISPENSED,
                DispenseLine.Status.FILLED_WITH_STOCK_OVERRIDE);
    }

    @Test
    void terminal_cancelled_and_returned() {
        assertThat(DispenseLineStatusRules.isTerminal(DispenseLine.Status.CANCELLED)).isTrue();
        assertThat(DispenseLineStatusRules.isTerminal(DispenseLine.Status.RETURNED)).isTrue();
        assertThat(DispenseLineStatusRules.isTerminal(DispenseLine.Status.DISPENSED)).isFalse();
    }
}
