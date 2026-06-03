package com.easyops.hospitalpharmacy.integration.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateChargeBatchPayload {
    private List<CreateChargePayload> charges;
}
