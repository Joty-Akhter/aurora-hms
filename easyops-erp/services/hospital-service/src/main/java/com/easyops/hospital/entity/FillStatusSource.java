package com.easyops.hospital.entity;

/**
 * Records whether fill status on a transmission was last updated from the external
 * e-prescribing network webhook or from in-house hospital pharmacy dispensing (Phase P2 — WS-B).
 */
public enum FillStatusSource {
    NETWORK_WEBHOOK,
    IN_HOUSE_PHARMACY
}
