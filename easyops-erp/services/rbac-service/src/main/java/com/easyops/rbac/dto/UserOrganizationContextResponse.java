package com.easyops.rbac.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Distinct organization IDs from active {@code user_roles} rows, plus whether the user has
 * any global (non-tenant) role assignments ({@code organization_id} is null).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganizationContextResponse {

    private List<UUID> organizationIds = new ArrayList<>();
    private boolean hasGlobalRoles;
}
