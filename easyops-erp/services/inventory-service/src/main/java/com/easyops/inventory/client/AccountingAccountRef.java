package com.easyops.inventory.client;

import lombok.Data;

/**
 * Minimal chart-of-accounts fields returned by accounting-service for GL code resolution.
 */
@Data
public class AccountingAccountRef {
    private String accountCode;
    private String accountName;
}
