package com.easyops.hospitalpharmacy.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Dispense queue: {@code HOSPITAL_PHARMACY_DISPENSE} ({@code hospital.pharmacy}/{@code dispense}) and
 * {@code hospital}/{@code manage}. Catalog / site master data: {@code hospital}/{@code view|manage}.
 * See {@code 002-hospital-permissions.sql}.
 */
@Service
public class HospitalPharmacyRbacService {

    private static final String RESOURCE_PHARMACY = "hospital.pharmacy";
    private static final String RESOURCE_HOSPITAL = "hospital";
    private static final String ACTION_DISPENSE = "dispense";
    private static final String ACTION_VIEW = "view";
    private static final String ACTION_MANAGE = "manage";

    /**
     * Read/search dispense orders: pharmacy {@code dispense}, or {@code hospital}/{@code manage}.
     * Excludes coarse {@code hospital}/{@code view} alone so e.g. {@code E_PRESCRIBING_TRANSMITTER} (transmit-only)
     * cannot list fulfillment queues without {@code HOSPITAL_PHARMACY_DISPENSE} or manage.
     */
    private static final String[][] PHARMACY_READ = {
            {RESOURCE_PHARMACY, ACTION_DISPENSE},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Create/update dispense lines, returns, status: dispense capability or full hospital manage. */
    private static final String[][] PHARMACY_DISPENSE_MUTATE = {
            {RESOURCE_PHARMACY, ACTION_DISPENSE},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Master data / catalog reads (drugs, manufacturers, pharmacy sites): coarse hospital access. */
    private static final String[][] HOSPITAL_VIEW = {
            {RESOURCE_HOSPITAL, ACTION_VIEW},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Master data writes (drug catalog, manufacturers, formulary, pharmacy locations). */
    private static final String[][] HOSPITAL_MANAGE = {
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Stock override (Phase P2) — {@code HOSPITAL_PHARMACY_STOCK_OVERRIDE} or hospital manage. */
    private static final String[][] PHARMACY_STOCK_OVERRIDE = {
            {"hospital.pharmacy", "stock_override"},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Approve stock requisition transfers between locations. */
    private static final String[][] PHARMACY_REQUISITION_APPROVE = {
            {"hospital.pharmacy", "requisition_approve"},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Approve emergency purchase entries. */
    private static final String[][] PHARMACY_EMERGENCY_PURCHASE_APPROVE = {
            {"hospital.pharmacy", "emergency_purchase_approve"},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Manage credit accounts and record payments. */
    private static final String[][] PHARMACY_CREDIT_MANAGE = {
            {"hospital.pharmacy", "credit_manage"},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Approve stock adjustment requests that exceed threshold. */
    private static final String[][] PHARMACY_STOCK_ADJUSTMENT_APPROVE = {
            {"hospital.pharmacy", "stock_adjustment_approve"},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    private final RbacPermissionClient rbac;

    public HospitalPharmacyRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requirePharmacyRead(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMACY_READ, "hp_pharmacy_read");
    }

    public void requirePharmacyDispenseMutate(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMACY_DISPENSE_MUTATE, "hp_pharmacy_dispense");
    }

    public void requireHospitalView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_VIEW, "hp_hospital_view");
    }

    public void requireHospitalManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_MANAGE, "hp_hospital_manage");
    }

    public void requireStockOverrideOrHospitalManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMACY_STOCK_OVERRIDE, "hp_pharmacy_stock_override");
    }

    public void requireRequisitionApprove(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMACY_REQUISITION_APPROVE, "hp_pharmacy_requisition_approve");
    }

    public void requireEmergencyPurchaseApprove(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMACY_EMERGENCY_PURCHASE_APPROVE, "hp_pharmacy_emergency_purchase_approve");
    }

    public void requireCreditManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMACY_CREDIT_MANAGE, "hp_pharmacy_credit_manage");
    }

    public void requireStockAdjustmentApprove(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMACY_STOCK_ADJUSTMENT_APPROVE, "hp_pharmacy_stock_adjustment_approve");
    }
}
