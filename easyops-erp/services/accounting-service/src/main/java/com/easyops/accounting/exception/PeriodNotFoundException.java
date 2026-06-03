package com.easyops.accounting.exception;

import java.time.LocalDate;
import java.util.UUID;

public class PeriodNotFoundException extends RuntimeException {

    public PeriodNotFoundException(UUID organizationId, LocalDate date) {
        super("No period found for date " + date + " in organization " + organizationId
                + ". Please set up fiscal years.");
    }
}
