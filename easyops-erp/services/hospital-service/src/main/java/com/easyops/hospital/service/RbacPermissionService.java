package com.easyops.hospital.service;

import com.easyops.rbac.client.RbacPermissionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Prescription-specific RBAC rules (Phase 1). Delegates to shared {@link RbacPermissionClient}.
 * Coarse {@code hospital} view/manage permissions remain valid for legacy roles (see Phase 0 seeds).
 */
@Service
@RequiredArgsConstructor
public class RbacPermissionService {

    private static final String RESOURCE_RX = "hospital.prescription";
    private static final String RESOURCE_PHARMACY = "hospital.pharmacy";
    private static final String RESOURCE_HOSPITAL = "hospital";
    private static final String RESOURCE_MENU = "hospital.menu";
    /** Doctor weekly slots / off-days → scheduling sync (Liquibase 102). */
    private static final String RESOURCE_DOCTOR_SCHEDULE = "hospital.doctor_schedule";
    private static final String ACTION_FEAT_CLINICAL_CHART = "feat_clinical_chart";
    private static final String ACTION_FEAT_DOCTOR_NOTES = "feat_doctor_notes";
    private static final String RESOURCE_DOCTOR_NOTES = "hospital.doctor_notes";
    private static final String ACTION_VIEW = "view";
    private static final String ACTION_MANAGE = "manage";
    private static final String ACTION_PRESCRIBE = "prescribe";
    private static final String ACTION_TRANSMIT = "transmit";

    private static final String[][] VIEW_ALTERNATIVES = {
            {RESOURCE_RX, ACTION_VIEW},
            {RESOURCE_HOSPITAL, ACTION_VIEW},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    private static final String[][] PRESCRIBE_ALTERNATIVES = {
            {RESOURCE_RX, ACTION_PRESCRIBE},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    private static final String[][] TRANSMIT_ALTERNATIVES = {
            {RESOURCE_RX, ACTION_TRANSMIT},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    private static final String[][] MANAGE_ALTERNATIVES = {
            {RESOURCE_RX, ACTION_MANAGE},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    private static final String[][] HOSPITAL_VIEW_ALTERNATIVES = {
            {RESOURCE_HOSPITAL, ACTION_VIEW},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    private static final String[][] HOSPITAL_MANAGE_ALTERNATIVES = {
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    private static final String[][] DOCTOR_SCHEDULE_MANAGE_ALTERNATIVES = {
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
            {RESOURCE_DOCTOR_SCHEDULE, ACTION_MANAGE},
    };

    /** Sidebar clinical chart catalog ({@code HOSPITAL_FEAT_CLINICAL_CHART}) or broad hospital access. */
    private static final String[][] CLINICAL_CHART_CATALOG_ALTERNATIVES = {
            {RESOURCE_MENU, ACTION_FEAT_CLINICAL_CHART},
            {RESOURCE_HOSPITAL, ACTION_VIEW},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Doctor broadcast notes list (call center, doctors, attendants, or broad hospital access). */
    private static final String[][] DOCTOR_NOTES_VIEW_ALTERNATIVES = {
            {RESOURCE_MENU, ACTION_FEAT_DOCTOR_NOTES},
            {RESOURCE_HOSPITAL, ACTION_VIEW},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** Post/edit/delete doctor broadcast notes (not granted to call center). */
    private static final String[][] DOCTOR_NOTES_MANAGE_ALTERNATIVES = {
            {RESOURCE_DOCTOR_NOTES, ACTION_MANAGE},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    /** EP investigations autosuggest: prescribers/viewers, hospital admins, or clinical chart catalog role. */
    private static final String[][] CLINICAL_CHART_INVESTIGATIONS_AUTOCOMPLETE_ALTERNATIVES = {
            {RESOURCE_RX, ACTION_VIEW},
            {RESOURCE_HOSPITAL, ACTION_VIEW},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
            {RESOURCE_MENU, ACTION_FEAT_CLINICAL_CHART},
    };

    private static final String[][] PHARMACY_DISPENSE_ALTERNATIVES = {
            {RESOURCE_PHARMACY, "dispense"},
            {RESOURCE_HOSPITAL, ACTION_MANAGE},
    };

    private final RbacPermissionClient rbac;

    public void requireAuthenticatedUser(UUID userId) {
        rbac.requireAuthenticatedUser(userId);
    }

    public void requirePrescriptionView(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, VIEW_ALTERNATIVES, "rx_view");
    }

    public void requirePrescriptionPrescribe(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, PRESCRIBE_ALTERNATIVES, "rx_prescribe");
    }

    public void requirePrescriptionTransmit(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, TRANSMIT_ALTERNATIVES, "rx_transmit");
    }

    /** Used for admin pharmacy directory management (create/update/deactivate/verify). */
    public void requirePrescriptionManage(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, MANAGE_ALTERNATIVES, "rx_manage");
    }

    /** In-house pharmacy → EHR fill sync (Phase P2); same actors as dispense queue. */
    public void requirePharmacyDispenseOrHospitalManage(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, PHARMACY_DISPENSE_ALTERNATIVES, "rx_pharmacy_in_house_fill");
    }

    public void requireHospitalView(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, HOSPITAL_VIEW_ALTERNATIVES, "hospital_view");
    }

    public void requireHospitalManage(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, HOSPITAL_MANAGE_ALTERNATIVES, "hospital_manage");
    }

    public boolean hasHospitalManage(UUID userId, UUID organizationId) {
        return rbac.hasAnyResourceAction(userId, organizationId, HOSPITAL_MANAGE_ALTERNATIVES);
    }

    public boolean hasDoctorScheduleManage(UUID userId, UUID organizationId) {
        return rbac.hasAnyResourceAction(userId, organizationId, DOCTOR_SCHEDULE_MANAGE_ALTERNATIVES);
    }

    public void requireDoctorScheduleManage(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, DOCTOR_SCHEDULE_MANAGE_ALTERNATIVES, "hospital_doctor_schedule_manage");
    }

    public void requireClinicalChartCatalogAccess(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, CLINICAL_CHART_CATALOG_ALTERNATIVES, "clinical_chart_catalog");
    }

    public void requireClinicalChartInvestigationsAutocomplete(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(
                userId, organizationId, CLINICAL_CHART_INVESTIGATIONS_AUTOCOMPLETE_ALTERNATIVES, "clinical_chart_inv_autocomplete");
    }

    public void requireDoctorNotesView(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, DOCTOR_NOTES_VIEW_ALTERNATIVES, "doctor_notes_view");
    }

    public void requireDoctorNotesManage(UUID userId, UUID organizationId) {
        rbac.requireAnyResourceAction(userId, organizationId, DOCTOR_NOTES_MANAGE_ALTERNATIVES, "doctor_notes_manage");
    }

    public boolean hasDoctorNotesManage(UUID userId, UUID organizationId) {
        return rbac.hasAnyResourceAction(userId, organizationId, DOCTOR_NOTES_MANAGE_ALTERNATIVES);
    }
}
