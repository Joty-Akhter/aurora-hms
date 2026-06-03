package com.easyops.hospitalclinicalorders.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class HospitalClinicalOrdersRbacService {

    private static final String[][] HOSPITAL_VIEW = {
            {"hospital", "view"},
            {"hospital", "manage"},
    };

    private static final String[][] HOSPITAL_MANAGE = {
            {"hospital", "manage"},
    };

    private final RbacPermissionClient rbac;

    public HospitalClinicalOrdersRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireHospitalView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_VIEW, "hospital_clinical_orders_view");
    }

    public void requireHospitalManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_MANAGE, "hospital_clinical_orders_manage");
    }
}
