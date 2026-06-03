package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebBookingAvailabilityResponse {
    private String date;
    private boolean blackedOut;
    private List<WebBookingSlotResponse> slots;
}
