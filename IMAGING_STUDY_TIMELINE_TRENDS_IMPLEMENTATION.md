# Imaging Study Timeline and Trends Implementation

## Overview

This document describes the implementation of Imaging Study Timeline and Trends features as specified in `EHR_PRESCRIPTION_MISSING_FEATURES.md` (lines 198-202). The implementation provides comprehensive timeline visualization, filtering, frequency tracking, interval analysis, and pattern identification.

## Implementation Date

January 2025

## Features Implemented

### ✅ 1. Chronological Timeline of Imaging Studies
- **ImagingStudyTimelineService**: Service for generating chronological timeline
- Timeline grouped by year-month periods
- Chronological ordering (newest first)
- Detailed study information in timeline view
- Interval calculation between consecutive studies

### ✅ 2. Filter by Study Type or Body Part
- Filter by modality (CT, MRI, XRAY, ULTRASOUND, etc.)
- Filter by body part examined
- Combined filtering support
- Dynamic filter options based on available data

### ✅ 3. Study Frequency and Interval Tracking
- **Frequency Statistics**:
  - Total studies count
  - Time span (days, months, years)
  - Average days/months/years between studies
  - Studies per year calculation
  - Earliest and latest study dates

- **Interval Statistics**:
  - Minimum interval between studies
  - Maximum interval between studies
  - Average interval between studies
  - Median interval between studies
  - Interval table showing all consecutive study pairs

### ✅ 4. Identify Study Patterns or Trends
- **Pattern Identification**:
  - Frequent studies pattern (more than 3 in a year)
  - Short intervals pattern (less than 30 days between studies)
  - Repeated body part pattern (same body part studied multiple times)
  - Repeated modality pattern (same modality used multiple times)
  - Pattern severity classification (LOW, MEDIUM, HIGH)

- **Trend Visualization**:
  - Studies by modality (bar chart)
  - Studies by body part (bar chart)
  - Studies over time (line chart by month)
  - Studies by year (statistics)

## Files Created

### Backend Services

1. **ImagingStudyTimelineService.java**
   - Location: `services/hospital-service/src/main/java/com/easyops/hospital/service/`
   - Handles timeline generation and trends analysis
   - Calculates intervals, frequency, and patterns
   - Provides filtering by modality and body part

### Backend DTOs

2. **ImagingStudyTimelineResponse.java**
   - Location: `services/hospital-service/src/main/java/com/easyops/hospital/dto/response/`
   - Response DTO for timeline data
   - Includes studies, intervals, and timeline by period

3. **ImagingStudyTrendResponse.java**
   - Location: `services/hospital-service/src/main/java/com/easyops/hospital/dto/response/`
   - Response DTO for trends analysis
   - Includes frequency stats, interval stats, patterns, and chart data

### Backend Controller Updates

4. **ImagingStudyController.java** (Updated)
   - Added `GET /api/imaging-studies/patients/{patientId}/timeline` endpoint
   - Added `GET /api/imaging-studies/patients/{patientId}/trends` endpoint
   - Supports optional modality and bodyPart query parameters

### Frontend Components

5. **ImagingStudyTimeline.tsx**
   - Location: `frontend/src/pages/hospital/`
   - Comprehensive timeline and trends visualization component
   - Two view modes: Timeline and Trends & Analysis
   - Filter controls for modality and body part
   - Charts using Recharts library
   - Interval tables and pattern identification

### Frontend Service Updates

6. **hospitalService.ts** (Updated)
   - Added `getImagingStudyTimeline(patientId, modality?, bodyPart?)` method
   - Added `getImagingStudyTrends(patientId, modality?, bodyPart?)` method

### Frontend Route Updates

7. **App.tsx** (Updated)
   - Added route: `/hospital/patients/:id/imaging-studies/timeline`
   - Added route: `/hospital/patients/:id/imaging-studies/:studyId`
   - Added route: `/hospital/patients/:id/imaging-studies`

8. **ImagingStudyResultsList.tsx** (Updated)
   - Added "Timeline & Trends" button in header
   - Navigation to timeline page

## API Endpoints

### Get Timeline
- **Endpoint**: `GET /api/imaging-studies/patients/{patientId}/timeline`
- **Query Parameters**:
  - `modality` (optional): Filter by modality (CT, MRI, XRAY, etc.)
  - `bodyPart` (optional): Filter by body part
- **Response**: `ImagingStudyTimelineResponse`
- **Description**: Returns chronological timeline with intervals

### Get Trends
- **Endpoint**: `GET /api/imaging-studies/patients/{patientId}/trends`
- **Query Parameters**:
  - `modality` (optional): Filter by modality
  - `bodyPart` (optional): Filter by body part
- **Response**: `ImagingStudyTrendResponse`
- **Description**: Returns trends analysis with frequency, intervals, patterns, and chart data

## Data Structures

### Timeline Response
```java
{
  patientId: UUID,
  studies: List<ImagingStudyResponse>,
  intervals: List<StudyInterval>,
  timelineByPeriod: Map<String, List<ImagingStudyResponse>>,
  totalStudies: int
}
```

### Trends Response
```java
{
  patientId: UUID,
  totalStudies: int,
  frequencyStats: {
    totalStudies: int,
    earliestStudyDate: LocalDateTime,
    latestStudyDate: LocalDateTime,
    timeSpanDays: long,
    timeSpanMonths: long,
    timeSpanYears: long,
    averageDaysBetween: Double,
    studiesPerYear: Double
  },
  intervalStats: {
    totalIntervals: int,
    minDaysBetween: long,
    maxDaysBetween: long,
    averageDaysBetween: double,
    medianDaysBetween: double
  },
  patterns: List<StudyPattern>,
  studiesByModality: Map<String, Long>,
  studiesByBodyPart: Map<String, Long>,
  studiesByYear: Map<String, Long>,
  studiesByMonth: Map<String, Long>
}
```

## User Interface

### Timeline View
- **Chronological Timeline**: Studies grouped by year-month periods
- **Study Table**: Detailed information for each study in the period
- **Intervals Table**: Shows intervals between consecutive studies
- **Filters**: Modality and body part dropdown filters

### Trends & Analysis View
- **Frequency Statistics Card**: Time span, studies per year, average intervals
- **Interval Statistics Card**: Min, max, average, and median intervals
- **Patterns Card**: List of identified patterns with severity indicators
- **Charts**:
  - Studies by Modality (Bar Chart)
  - Studies by Body Part (Bar Chart)
  - Studies Over Time (Line Chart by month)

### Filtering
- **Modality Filter**: Dropdown with all available modalities
- **Body Part Filter**: Dropdown with all available body parts
- **Clear Filters Button**: Resets all filters
- **Real-time Updates**: Data refreshes when filters change

## Pattern Identification

The system identifies the following patterns:

1. **FREQUENT_STUDIES**: More than 3 studies in a single year
   - Severity: MEDIUM
   - Description: "High frequency: X studies in year Y"

2. **SHORT_INTERVALS**: Studies with intervals less than 30 days
   - Severity: LOW
   - Description: "X studies with intervals less than 30 days"

3. **REPEATED_BODY_PART**: Same body part studied more than 3 times
   - Severity: LOW
   - Description: "Body part 'X' studied Y times"

4. **REPEATED_MODALITY**: Same modality used more than 5 times
   - Severity: LOW
   - Description: "Modality 'X' used Y times"

## Usage Examples

### Access Timeline from List View
1. Navigate to patient's imaging studies list
2. Click "Timeline & Trends" button in header
3. View chronological timeline or switch to trends view

### Filter Timeline
1. Select modality from dropdown (e.g., "CT")
2. Select body part from dropdown (e.g., "Chest")
3. Timeline and trends update automatically

### View Trends
1. Click "Trends & Analysis" tab
2. Review frequency and interval statistics
3. View charts for visual analysis
4. Check identified patterns

## Testing Recommendations

1. Test timeline with various numbers of studies (0, 1, 2, many)
2. Test filtering by modality
3. Test filtering by body part
4. Test combined filters
5. Verify interval calculations
6. Test pattern identification
7. Verify chart rendering with different data sets
8. Test with studies spanning multiple years
9. Test with studies having same dates
10. Verify responsive design on different screen sizes

## Status

✅ **100% Complete** - All features from lines 198-202 of `EHR_PRESCRIPTION_MISSING_FEATURES.md` have been implemented:
- ✅ Chronological timeline of imaging studies - IMPLEMENTED
- ✅ Filter by study type or body part - IMPLEMENTED
- ✅ Study frequency and interval tracking - IMPLEMENTED
- ✅ Identify study patterns or trends - IMPLEMENTED

## Notes

1. **Timeline Grouping**: Studies are grouped by year-month for easier navigation and visualization.

2. **Interval Calculation**: Intervals are calculated between consecutive studies when sorted chronologically.

3. **Pattern Detection**: Patterns are identified based on configurable thresholds (e.g., 3 studies per year, 30 days interval).

4. **Chart Library**: Uses Recharts library (already available in the project) for visualization.

5. **Performance**: Timeline and trends calculations are performed server-side for better performance with large datasets.

6. **Filtering**: Filters can be combined (modality AND body part) or used independently.

7. **Real-time Updates**: Data automatically refreshes when filters change, providing immediate feedback.
