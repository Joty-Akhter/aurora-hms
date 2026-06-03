package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Combined ad-hoc drug interaction + allergy screening for pharmacy dispense (Phase P4 — WS-I).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispenseClinicalScreenResponse {

    private DrugInteractionCheckResponse interactions;
    private AllergyCheckResponse allergies;
}
