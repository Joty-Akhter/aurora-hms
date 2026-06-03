package com.easyops.accounting.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "accounting.validation")
public class AccountingValidationProperties {

    private boolean enforceBalance = true;
    private boolean allowBackdated = false;
}
