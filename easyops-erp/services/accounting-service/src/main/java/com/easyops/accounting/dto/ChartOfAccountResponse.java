package com.easyops.accounting.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ChartOfAccountResponse {

    UUID id;
    UUID organizationId;
    String accountCode;
    String accountName;
    UUID parentAccountId;
    String accountType;
    String accountCategory;
    String accountSubcategory;
    Integer level;
    Boolean isGroup;
    Boolean isSystemAccount;
    String currency;
    BigDecimal openingBalance;
    LocalDate openingBalanceDate;
    BigDecimal currentBalance;
    Boolean isActive;
    Boolean allowManualEntry;
    String description;
    String taxType;
    List<String> tags;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    UUID createdBy;
    UUID updatedBy;
}


