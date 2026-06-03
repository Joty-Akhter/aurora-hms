package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PrescriptionTransmission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InHouseDispenseFillResponse {

    private UUID prescriptionId;
    private UUID dispenseOrderId;
    private PrescriptionTransmission.FillStatus fillStatus;
    private UUID transmissionId;
    private boolean transmissionUpdated;
}
