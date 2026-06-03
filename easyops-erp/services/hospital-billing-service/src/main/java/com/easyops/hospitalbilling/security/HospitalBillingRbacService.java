package com.easyops.hospitalbilling.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Aligns with {@code HOSPITAL_VIEW} / {@code HOSPITAL_MANAGE} ({@code hospital}/{@code view|manage} in
 * hospital permission seeds).
 */
@Service
public class HospitalBillingRbacService {

    private static final String[][] HOSPITAL_VIEW = {
            {"hospital", "view"},
            {"hospital", "manage"},
    };

    private static final String[][] HOSPITAL_MANAGE = {
            {"hospital", "manage"},
    };

    /** Phase P2 — pharmacy dispense charge posting or full hospital manage. */
    private static final String[][] CHARGE_POST_ALTERNATIVES = {
            {"hospital", "manage"},
            {"hospital.pharmacy", "charge_post"},
    };

    private final RbacPermissionClient rbac;

    public HospitalBillingRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireHospitalView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_VIEW, "hospital_view");
    }

    public void requireHospitalManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_MANAGE, "hospital_manage");
    }

    public void requireChargePostOrHospitalManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, CHARGE_POST_ALTERNATIVES, "billing_charge_post");
    }
}
