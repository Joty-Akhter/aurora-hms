# Imaging Report Printing and PDF Export Implementation

## Overview

This document describes the implementation of Imaging Report Printing and PDF Export features as specified in `EHR_PRESCRIPTION_MISSING_FEATURES.md` (lines 191-196). The implementation provides comprehensive printing and PDF export capabilities for imaging study reports.

## Implementation Date

January 2025

## Features Implemented

### ✅ 1. Report Printing
- **ImagingReportPrintService**: Service for generating printable HTML reports
- Print-friendly HTML generation with professional styling
- Browser-based printing functionality
- Print-optimized CSS with page break controls
- Professional report layout with headers, sections, and footers

### ✅ 2. Report Export (PDF)
- **ImagingReportExportService**: Service for exporting reports to PDF format
- PDF generation using iText7 html2pdf
- Automatic filename generation based on accession number
- Professional PDF formatting matching print layout

## Files Created

### Backend Services

1. **ImagingReportPrintService.java**
   - Location: `services/hospital-service/src/main/java/com/easyops/hospital/service/`
   - Generates printable HTML reports
   - Includes professional styling for print media
   - Handles all report sections (patient info, study info, findings, impression, etc.)

2. **ImagingReportExportService.java**
   - Location: `services/hospital-service/src/main/java/com/easyops/hospital/service/`
   - Converts HTML reports to PDF using iText7
   - Handles PDF byte array generation
   - Provides filename generation

### Backend Controller Updates

3. **ImagingStudyController.java** (Updated)
   - Added `GET /api/imaging-studies/{studyId}/print` endpoint
   - Added `GET /api/imaging-studies/{studyId}/export/pdf` endpoint

### Frontend Updates

4. **hospitalService.ts** (Updated)
   - Added `getPrintableReport(studyId)` method
   - Added `exportReportToPdf(studyId)` method

5. **ImagingStudyDetail.tsx** (Updated)
   - Added Print button with print icon
   - Added Export PDF button with PDF icon
   - Implemented `handlePrint()` function
   - Implemented `handleExportPdf()` function

6. **ImagingStudyResultsList.tsx** (Updated)
   - Added Print icon button in actions column
   - Added Export PDF icon button in actions column
   - Implemented `handlePrint()` function
   - Implemented `handleExportPdf()` function

## Dependencies Added

### pom.xml Updates

Added the following PDF generation library:

```xml
<!-- PDF Generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>html2pdf</artifactId>
    <version>5.0.2</version>
</dependency>
```

## API Endpoints

### Print Report
- **Endpoint**: `GET /api/imaging-studies/{studyId}/print`
- **Response**: HTML content (text/html)
- **Description**: Returns printable HTML version of the imaging study report
- **Usage**: Opens in new window and triggers browser print dialog

### Export to PDF
- **Endpoint**: `GET /api/imaging-studies/{studyId}/export/pdf`
- **Response**: PDF file (application/pdf)
- **Description**: Returns PDF file of the imaging study report
- **Usage**: Downloads PDF file with filename based on accession number

## Report Format

The generated reports include:

1. **Report Header**
   - Title: "IMAGING STUDY REPORT"
   - Accession Number
   - Study Number
   - Report Date

2. **Patient Information**
   - Patient Name
   - MRN
   - Patient ID

3. **Study Information**
   - Study Name
   - Modality
   - CPT Code
   - Body Part
   - Study Date
   - Completion Date
   - Status
   - Contrast information
   - Number of images/series

4. **Report Content**
   - Clinical History
   - Technique Description
   - Findings (highlighted box)
   - Impression/Conclusion (highlighted box)
   - Recommendations (highlighted box)

5. **Radiologist Information**
   - Interpreting Radiologist
   - NPI
   - Specialty
   - Preliminary Reading By
   - Reviewing Radiologist
   - Finalized Date

6. **Footer**
   - Electronic signature notice
   - Final/Preliminary indicator

## Print Styling

The print styles include:
- Professional typography (Arial, 11pt)
- Proper page margins (1 inch)
- Page break controls to avoid breaking sections
- Color-coded sections (findings, impression, recommendations)
- Table formatting for structured data
- Print-optimized layout

## Frontend Implementation

### Print Functionality
- Opens printable HTML in new window
- Automatically triggers browser print dialog
- Uses browser's native print capabilities

### PDF Export Functionality
- Downloads PDF file directly
- Filename format: `Imaging_Report_{AccessionNumber}.pdf`
- Shows success/error notifications
- Handles blob download properly

## Usage Examples

### Print Report (Frontend)
```typescript
const handlePrint = async () => {
  const response = await hospitalService.getPrintableReport(studyId);
  const printWindow = window.open('', '_blank');
  if (printWindow) {
    printWindow.document.write(response.data);
    printWindow.document.close();
    printWindow.onload = () => {
      printWindow.print();
    };
  }
};
```

### Export PDF (Frontend)
```typescript
const handleExportPdf = async () => {
  const response = await hospitalService.exportReportToPdf(studyId);
  const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `Imaging_Report_${accessionNumber}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};
```

## User Interface

### ImagingStudyDetail Page
- **Print Button**: Located in the header next to "View Images" button
- **Export PDF Button**: Located in the header next to Print button
- Both buttons are visible for all studies

### ImagingStudyResultsList Page
- **Print Icon**: Action button in the table row
- **Export PDF Icon**: Action button in the table row
- Tooltips: "Print Report" and "Export PDF"
- Both icons are available for each study in the list

## Testing Recommendations

1. Test print functionality with various report types
2. Verify PDF export generates correct files
3. Test with different report statuses (Final, Preliminary, Amended)
4. Verify print layout on different browsers
5. Test PDF file download and opening
6. Verify filename generation
7. Test with reports containing special characters
8. Verify print styles render correctly
9. Test page breaks in long reports
10. Verify all report sections are included

## Status

✅ **100% Complete** - All features from lines 191-196 of `EHR_PRESCRIPTION_MISSING_FEATURES.md` have been implemented:
- ✅ Report printing - IMPLEMENTED
- ✅ Report export (PDF) - IMPLEMENTED

## Notes

1. **Print Functionality**: Uses browser's native print dialog, which provides users with full control over printing options (printer selection, page range, etc.)

2. **PDF Generation**: Uses iText7 html2pdf which converts HTML to PDF. The HTML is generated server-side to ensure consistent formatting.

3. **File Naming**: PDF files are named using the accession number when available, falling back to study ID if accession number is not available.

4. **Error Handling**: Both print and export functions include error handling with user-friendly error messages via snackbar notifications.

5. **Browser Compatibility**: Print functionality works in all modern browsers. PDF export requires proper blob handling support.
