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
 * Response DTO for imaging study trends analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingStudyTrendResponse {
    
    private UUID patientId;
    private int totalStudies;
    private ImagingStudyTimelineService.FrequencyStats frequencyStats;
    private ImagingStudyTimelineService.IntervalStats intervalStats;
    private List<ImagingStudyTimelineService.StudyPattern> patterns;
    private Map<String, Long> studiesByModality;
    private Map<String, Long> studiesByBodyPart;
    private Map<String, Long> studiesByYear;
    private Map<String, Long> studiesByMonth;
}
