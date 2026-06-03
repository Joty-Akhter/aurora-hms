package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.ImagingStudyResponse;
import com.easyops.hospital.dto.response.ImagingStudyTimelineResponse;
import com.easyops.hospital.dto.response.ImagingStudyTrendResponse;
import com.easyops.hospital.entity.ImagingStudy;
import com.easyops.hospital.repository.ImagingStudyRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for imaging study timeline and trends analysis.
 * Provides chronological timeline, frequency tracking, interval analysis, and pattern identification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImagingStudyTimelineService {
    
    private final ImagingStudyRepository imagingStudyRepository;
    private final ImagingStudyService imagingStudyService;
    
    /**
     * Get chronological timeline of imaging studies for a patient
     */
    public ImagingStudyTimelineResponse getTimeline(UUID patientId, String modality, String bodyPart) {
        log.info("Getting timeline for patient: {}, modality: {}, bodyPart: {}", patientId, modality, bodyPart);
        
        List<ImagingStudy> studies;
        
        // Apply filters
        if (modality != null && !modality.isEmpty()) {
            try {
                ImagingStudy.StudyModality mod = ImagingStudy.StudyModality.valueOf(modality.toUpperCase());
                studies = imagingStudyRepository.findStudiesByModality(patientId, mod);
            } catch (IllegalArgumentException e) {
                studies = imagingStudyRepository.findByPatientPatientIdOrderByStudyDateDesc(patientId);
            }
        } else if (bodyPart != null && !bodyPart.isEmpty()) {
            studies = imagingStudyRepository.findStudiesByBodyPart(patientId, bodyPart);
        } else {
            studies = imagingStudyRepository.findByPatientPatientIdOrderByStudyDateDesc(patientId);
        }
        
        // Convert to response DTOs - use existing service method that returns list
        List<ImagingStudyResponse> studyResponses = imagingStudyService.getImagingStudiesByPatient(patientId);
        
        // Apply filters if specified
        if (modality != null && !modality.isEmpty()) {
            try {
                ImagingStudy.StudyModality mod = ImagingStudy.StudyModality.valueOf(modality.toUpperCase());
                studyResponses = studyResponses.stream()
                    .filter(s -> s.getStudyModality() == mod)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid modality, keep all studies
            }
        }
        
        if (bodyPart != null && !bodyPart.isEmpty()) {
            studyResponses = studyResponses.stream()
                .filter(s -> s.getBodyPartExamined() != null && s.getBodyPartExamined().equalsIgnoreCase(bodyPart))
                .collect(Collectors.toList());
        }
        
        // Sort by date descending for timeline
        studyResponses.sort((a, b) -> b.getStudyDate().compareTo(a.getStudyDate()));
        
        // Calculate intervals
        List<StudyInterval> intervals = calculateIntervals(studies);
        
        // Group by year/month for timeline view
        Map<String, List<ImagingStudyResponse>> timelineByPeriod = groupByPeriod(studyResponses);
        
        return ImagingStudyTimelineResponse.builder()
            .patientId(patientId)
            .studies(studyResponses)
            .intervals(intervals)
            .timelineByPeriod(timelineByPeriod)
            .totalStudies(studies.size())
            .build();
    }
    
    /**
     * Get trends analysis for imaging studies
     */
    public ImagingStudyTrendResponse getTrends(UUID patientId, String modality, String bodyPart) {
        log.info("Getting trends for patient: {}, modality: {}, bodyPart: {}", patientId, modality, bodyPart);
        
        // Get all studies for patient first
        List<ImagingStudy> studies = imagingStudyRepository.findByPatientPatientIdOrderByStudyDateDesc(patientId);
        
        // Apply filters
        if (modality != null && !modality.isEmpty()) {
            try {
                ImagingStudy.StudyModality mod = ImagingStudy.StudyModality.valueOf(modality.toUpperCase());
                studies = studies.stream()
                    .filter(s -> s.getStudyModality() == mod)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid modality, keep all studies
            }
        }
        
        if (bodyPart != null && !bodyPart.isEmpty()) {
            studies = studies.stream()
                .filter(s -> s.getBodyPartExamined() != null && s.getBodyPartExamined().equalsIgnoreCase(bodyPart))
                .collect(Collectors.toList());
        }
        
        // Calculate frequency statistics
        FrequencyStats frequencyStats = calculateFrequencyStats(studies);
        
        // Calculate interval statistics
        IntervalStats intervalStats = calculateIntervalStats(studies);
        
        // Identify patterns
        List<StudyPattern> patterns = identifyPatterns(studies);
        
        // Group by modality
        Map<String, Long> studiesByModality = studies.stream()
            .collect(Collectors.groupingBy(
                s -> s.getStudyModality().toString(),
                Collectors.counting()
            ));
        
        // Group by body part
        Map<String, Long> studiesByBodyPart = studies.stream()
            .collect(Collectors.groupingBy(
                ImagingStudy::getBodyPartExamined,
                Collectors.counting()
            ));
        
        // Time-based trends (by year/month)
        Map<String, Long> studiesByYear = studies.stream()
            .collect(Collectors.groupingBy(
                s -> String.valueOf(s.getStudyDate().getYear()),
                Collectors.counting()
            ));
        
        Map<String, Long> studiesByMonth = studies.stream()
            .collect(Collectors.groupingBy(
                s -> s.getStudyDate().getYear() + "-" + String.format("%02d", s.getStudyDate().getMonthValue()),
                Collectors.counting()
            ));
        
        return ImagingStudyTrendResponse.builder()
            .patientId(patientId)
            .totalStudies(studies.size())
            .frequencyStats(frequencyStats)
            .intervalStats(intervalStats)
            .patterns(patterns)
            .studiesByModality(studiesByModality)
            .studiesByBodyPart(studiesByBodyPart)
            .studiesByYear(studiesByYear)
            .studiesByMonth(studiesByMonth)
            .build();
    }
    
    /**
     * Calculate intervals between consecutive studies
     */
    private List<StudyInterval> calculateIntervals(List<ImagingStudy> studies) {
        List<StudyInterval> intervals = new ArrayList<>();
        
        if (studies.size() < 2) {
            return intervals;
        }
        
        // Sort by date (oldest first)
        List<ImagingStudy> sortedStudies = studies.stream()
            .sorted(Comparator.comparing(ImagingStudy::getStudyDate))
            .collect(Collectors.toList());
        
        for (int i = 1; i < sortedStudies.size(); i++) {
            ImagingStudy previous = sortedStudies.get(i - 1);
            ImagingStudy current = sortedStudies.get(i);
            
            long daysBetween = ChronoUnit.DAYS.between(previous.getStudyDate(), current.getStudyDate());
            long monthsBetween = ChronoUnit.MONTHS.between(previous.getStudyDate(), current.getStudyDate());
            long yearsBetween = ChronoUnit.YEARS.between(previous.getStudyDate(), current.getStudyDate());
            
            StudyInterval interval = new StudyInterval();
            interval.setPreviousStudyId(previous.getStudyId());
            interval.setPreviousStudyDate(previous.getStudyDate());
            interval.setPreviousStudyName(previous.getStudyName());
            interval.setCurrentStudyId(current.getStudyId());
            interval.setCurrentStudyDate(current.getStudyDate());
            interval.setCurrentStudyName(current.getStudyName());
            interval.setDaysBetween(daysBetween);
            interval.setMonthsBetween(monthsBetween);
            interval.setYearsBetween(yearsBetween);
            
            intervals.add(interval);
        }
        
        return intervals;
    }
    
    /**
     * Group studies by time period (year-month)
     */
    private Map<String, List<ImagingStudyResponse>> groupByPeriod(List<ImagingStudyResponse> studies) {
        return studies.stream()
            .collect(Collectors.groupingBy(
                s -> {
                    LocalDateTime date = s.getStudyDate();
                    return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                },
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }
    
    /**
     * Calculate frequency statistics
     */
    private FrequencyStats calculateFrequencyStats(List<ImagingStudy> studies) {
        FrequencyStats stats = new FrequencyStats();
        stats.setTotalStudies(studies.size());
        
        if (studies.isEmpty()) {
            return stats;
        }
        
        // Calculate time span
        LocalDateTime earliest = studies.stream()
            .map(ImagingStudy::getStudyDate)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        LocalDateTime latest = studies.stream()
            .map(ImagingStudy::getStudyDate)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        long totalDays = ChronoUnit.DAYS.between(earliest, latest);
        long totalMonths = ChronoUnit.MONTHS.between(earliest, latest);
        long totalYears = ChronoUnit.YEARS.between(earliest, latest);
        
        stats.setEarliestStudyDate(earliest);
        stats.setLatestStudyDate(latest);
        stats.setTimeSpanDays(totalDays);
        stats.setTimeSpanMonths(totalMonths);
        stats.setTimeSpanYears(totalYears);
        
        // Calculate average frequency
        if (totalDays > 0) {
            stats.setAverageDaysBetween((double) totalDays / Math.max(1, studies.size() - 1));
        }
        if (totalMonths > 0) {
            stats.setAverageMonthsBetween((double) totalMonths / Math.max(1, studies.size() - 1));
        }
        if (totalYears > 0) {
            stats.setAverageYearsBetween((double) totalYears / Math.max(1, studies.size() - 1));
        }
        
        // Studies per year
        if (totalYears > 0) {
            stats.setStudiesPerYear((double) studies.size() / (totalYears + 1));
        } else if (totalMonths > 0) {
            stats.setStudiesPerYear((double) studies.size() / ((totalMonths + 1) / 12.0));
        } else {
            stats.setStudiesPerYear((double) studies.size());
        }
        
        return stats;
    }
    
    /**
     * Calculate interval statistics
     */
    private IntervalStats calculateIntervalStats(List<ImagingStudy> studies) {
        IntervalStats stats = new IntervalStats();
        
        List<StudyInterval> intervals = calculateIntervals(studies);
        
        if (intervals.isEmpty()) {
            return stats;
        }
        
        List<Long> daysList = intervals.stream()
            .map(StudyInterval::getDaysBetween)
            .collect(Collectors.toList());
        
        stats.setTotalIntervals(intervals.size());
        stats.setMinDaysBetween(daysList.stream().mapToLong(Long::longValue).min().orElse(0));
        stats.setMaxDaysBetween(daysList.stream().mapToLong(Long::longValue).max().orElse(0));
        stats.setAverageDaysBetween(daysList.stream().mapToLong(Long::longValue).average().orElse(0));
        
        // Median
        Collections.sort(daysList);
        int middle = daysList.size() / 2;
        if (daysList.size() % 2 == 0) {
            stats.setMedianDaysBetween((daysList.get(middle - 1) + daysList.get(middle)) / 2.0);
        } else {
            stats.setMedianDaysBetween((double) daysList.get(middle));
        }
        
        return stats;
    }
    
    /**
     * Identify patterns in study history
     */
    private List<StudyPattern> identifyPatterns(List<ImagingStudy> studies) {
        List<StudyPattern> patterns = new ArrayList<>();
        
        if (studies.size() < 2) {
            return patterns;
        }
        
        // Pattern 1: Frequent studies (more than 3 in a year)
        Map<Integer, Long> studiesByYear = studies.stream()
            .collect(Collectors.groupingBy(
                s -> s.getStudyDate().getYear(),
                Collectors.counting()
            ));
        
        studiesByYear.entrySet().stream()
            .filter(e -> e.getValue() > 3)
            .forEach(e -> {
                StudyPattern pattern = new StudyPattern();
                pattern.setType("FREQUENT_STUDIES");
                pattern.setDescription(String.format("High frequency: %d studies in year %d", e.getValue(), e.getKey()));
                pattern.setSeverity("MEDIUM");
                patterns.add(pattern);
            });
        
        // Pattern 2: Short intervals (less than 30 days between studies)
        List<StudyInterval> intervals = calculateIntervals(studies);
        long shortIntervals = intervals.stream()
            .filter(i -> i.getDaysBetween() < 30)
            .count();
        
        if (shortIntervals > 0) {
            StudyPattern pattern = new StudyPattern();
            pattern.setType("SHORT_INTERVALS");
            pattern.setDescription(String.format("%d studies with intervals less than 30 days", shortIntervals));
            pattern.setSeverity("LOW");
            patterns.add(pattern);
        }
        
        // Pattern 3: Same body part repeated
        Map<String, Long> bodyPartFrequency = studies.stream()
            .collect(Collectors.groupingBy(
                ImagingStudy::getBodyPartExamined,
                Collectors.counting()
            ));
        
        bodyPartFrequency.entrySet().stream()
            .filter(e -> e.getValue() > 3)
            .forEach(e -> {
                StudyPattern pattern = new StudyPattern();
                pattern.setType("REPEATED_BODY_PART");
                pattern.setDescription(String.format("Body part '%s' studied %d times", e.getKey(), e.getValue()));
                pattern.setSeverity("LOW");
                patterns.add(pattern);
            });
        
        // Pattern 4: Same modality repeated
        Map<String, Long> modalityFrequency = studies.stream()
            .collect(Collectors.groupingBy(
                s -> s.getStudyModality().toString(),
                Collectors.counting()
            ));
        
        modalityFrequency.entrySet().stream()
            .filter(e -> e.getValue() > 5)
            .forEach(e -> {
                StudyPattern pattern = new StudyPattern();
                pattern.setType("REPEATED_MODALITY");
                pattern.setDescription(String.format("Modality '%s' used %d times", e.getKey(), e.getValue()));
                pattern.setSeverity("LOW");
                patterns.add(pattern);
            });
        
        return patterns;
    }
    
    // Inner classes for data structures
    
    @Data
    public static class StudyInterval {
        private UUID previousStudyId;
        private LocalDateTime previousStudyDate;
        private String previousStudyName;
        private UUID currentStudyId;
        private LocalDateTime currentStudyDate;
        private String currentStudyName;
        private long daysBetween;
        private long monthsBetween;
        private long yearsBetween;
    }
    
    @Data
    public static class FrequencyStats {
        private int totalStudies;
        private LocalDateTime earliestStudyDate;
        private LocalDateTime latestStudyDate;
        private long timeSpanDays;
        private long timeSpanMonths;
        private long timeSpanYears;
        private Double averageDaysBetween;
        private Double averageMonthsBetween;
        private Double averageYearsBetween;
        private Double studiesPerYear;
    }
    
    @Data
    public static class IntervalStats {
        private int totalIntervals;
        private long minDaysBetween;
        private long maxDaysBetween;
        private double averageDaysBetween;
        private double medianDaysBetween;
    }
    
    @Data
    public static class StudyPattern {
        private String type;
        private String description;
        private String severity; // LOW, MEDIUM, HIGH
    }
}
