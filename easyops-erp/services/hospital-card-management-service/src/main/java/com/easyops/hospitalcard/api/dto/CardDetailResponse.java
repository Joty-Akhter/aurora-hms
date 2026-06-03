package com.easyops.hospitalcard.api.dto;

/**
 * Extends CardResponse with accountSummary for GET /cards/{id}.
 */
public class CardDetailResponse extends CardResponse {

    private AccountSummary accountSummary;

    public AccountSummary getAccountSummary() {
        return accountSummary;
    }

    public void setAccountSummary(AccountSummary accountSummary) {
        this.accountSummary = accountSummary;
    }
}
