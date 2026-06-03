package com.easyops.hospitalbilling.api.dto;

import jakarta.validation.Valid;
import java.util.List;

public class CreateChargeBatchRequest {

    @Valid
    private List<CreateChargeRequest> charges;

    public List<CreateChargeRequest> getCharges() {
        return charges;
    }

    public void setCharges(List<CreateChargeRequest> charges) {
        this.charges = charges;
    }
}

