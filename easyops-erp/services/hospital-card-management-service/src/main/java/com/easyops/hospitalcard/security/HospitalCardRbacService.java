package com.easyops.hospitalcard.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class HospitalCardRbacService {

    private static final String[][] HOSPITAL_VIEW = {
            {"hospital", "view"},
            {"hospital", "manage"},
    };

    private static final String[][] HOSPITAL_MANAGE = {
            {"hospital", "manage"},
    };

    private final RbacPermissionClient rbac;

    public HospitalCardRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireHospitalView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_VIEW, "hospital_card_view");
    }

    public void requireHospitalManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_MANAGE, "hospital_card_manage");
    }
}
