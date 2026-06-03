package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientTimelineResponse {
    
    private UUID patientId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<TimelineEvent> events;
    private Integer totalEvents;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        private LocalDate eventDate;
        private LocalTime eventTime;
        private String eventType; // VITAL_SIGNS, CLINICAL_NOTE, PRESCRIPTION, PROBLEM, etc.
        private String title;
        private String description;
    }
}
