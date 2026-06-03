package com.easyops.hospital.dto.response;

import com.easyops.hospital.service.ImagingStudyTimelineService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for imaging study timeline
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingStudyTimelineResponse {
    
    private UUID patientId;
    private List<ImagingStudyResponse> studies;
    private List<ImagingStudyTimelineService.StudyInterval> intervals;
    private Map<String, List<ImagingStudyResponse>> timelineByPeriod;
    private int totalStudies;
}
