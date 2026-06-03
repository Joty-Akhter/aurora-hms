package com.easyops.ar.client;

import java.util.UUID;

public interface BankGlAccountResolver {
    UUID resolveGlAccountId(UUID bankAccountId, UUID actorUserId);
}
