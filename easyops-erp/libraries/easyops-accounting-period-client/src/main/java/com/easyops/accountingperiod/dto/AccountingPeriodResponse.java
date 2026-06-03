package com.easyops.accountingperiod.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountingPeriodResponse {
    private UUID id;
}
