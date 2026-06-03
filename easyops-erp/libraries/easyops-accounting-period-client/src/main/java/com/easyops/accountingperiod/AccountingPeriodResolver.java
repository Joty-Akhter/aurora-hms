package com.easyops.accountingperiod;

import java.time.LocalDate;
import java.util.UUID;

public interface AccountingPeriodResolver {

    UUID resolvePeriodId(UUID organizationId, LocalDate date, UUID actorUserId);
}
