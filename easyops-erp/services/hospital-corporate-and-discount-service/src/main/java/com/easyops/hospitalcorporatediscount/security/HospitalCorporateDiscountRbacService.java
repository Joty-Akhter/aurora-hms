package com.easyops.hospitalcorporatediscount.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Same coarse hospital permissions as {@code hospital-billing-service}: {@code HOSPITAL_VIEW} /
 * {@code HOSPITAL_MANAGE} ({@code hospital} / {@code view|manage}).
 */
@Service
public class HospitalCorporateDiscountRbacService {

    private static final String[][] HOSPITAL_VIEW = {
            {"hospital", "view"},
            {"hospital", "manage"},
    };

    private static final String[][] HOSPITAL_MANAGE = {
            {"hospital", "manage"},
    };

    private final RbacPermissionClient rbac;

    public HospitalCorporateDiscountRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireHospitalView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_VIEW, "hospital_view");
    }

    public void requireHospitalManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_MANAGE, "hospital_manage");
    }
}
