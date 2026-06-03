package com.easyops.hospital.controller;

import com.easyops.hospital.dto.response.DICOMImageResponse;
import com.easyops.hospital.dto.response.DICOMMetadataResponse;
import com.easyops.hospital.entity.ImagingImageAttachment;
import com.easyops.hospital.repository.ImagingImageAttachmentRepository;
import com.easyops.hospital.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for DICOM image management operations.
 * Handles DICOM file upload, storage, retrieval, compression, and network operations.
 */
@RestController
@RequestMapping("/api/dicom")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DICOM Image Management", description = "DICOM image storage, retrieval, compression, and network operations")
@CrossOrigin(origins = "*")
public class DICOMImageController {
    
    private final DICOMImageStorageService dicomImageStorageService;
    private final DICOMMetadataService dicomMetadataService;
    private final DICOMNetworkService dicomNetworkService;
    private final DICOMCompressionService dicomCompressionService;
    private final DICOMThumbnailService dicomThumbnailService;
    private final ImagingImageAttachmentRepository imageAttachmentRepository;
    
    // ========== DICOM File Storage ==========
    
    @PostMapping("/images/{studyId}/upload")
    @Operation(summary = "Upload DICOM file", description = "Upload a DICOM Part 10 file for an imaging study")
    public ResponseEntity<DICOMImageResponse> uploadDicomFile(
            @PathVariable UUID studyId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        log.info("Uploading DICOM file for study: {}", studyId);
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            ImagingImageAttachment attachment = dicomImageStorageService.storeDicomFile(studyId, file, userId);
            
            // Generate thumbnail
            File dicomFile = new File(attachment.getFilePath());
            File thumbnailFile = dicomThumbnailService.generateThumbnail(
                dicomFile, studyId, attachment.getDicomSopInstanceUid());
            
            if (thumbnailFile != null) {
                attachment.setThumbnailPath(thumbnailFile.getAbsolutePath());
                attachment.setThumbnailUrl(dicomThumbnailService.getThumbnailUrl(attachment.getAttachmentId()));
                imageAttachmentRepository.save(attachment);
            }
            
            return ResponseEntity.ok(mapToResponse(attachment));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid DICOM upload request for study {}: {}", studyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (msg.contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            log.error("Failed to upload DICOM file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Failed to upload DICOM file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/images/{attachmentId}/download")
    @Operation(summary = "Download DICOM file", description = "Download a DICOM file by attachment ID")
    public ResponseEntity<Resource> downloadDicomFile(@PathVariable UUID attachmentId) {
        log.info("Downloading DICOM file: {}", attachmentId);
        
        try {
            File dicomFile = dicomImageStorageService.retrieveDicomFile(attachmentId);
            Resource resource = new FileSystemResource(dicomFile);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/dicom"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dicomFile.getName() + "\"")
                .body(resource);
        } catch (FileNotFoundException e) {
            log.error("DICOM file not found: {}", attachmentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to download DICOM file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/images/{attachmentId}")
    @Operation(summary = "Delete DICOM file", description = "Delete a DICOM file and its thumbnail")
    public ResponseEntity<Void> deleteDicomFile(@PathVariable UUID attachmentId) {
        log.info("Deleting DICOM file: {}", attachmentId);
        
        try {
            dicomImageStorageService.deleteDicomFile(attachmentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Failed to delete DICOM file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Failed to delete DICOM file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/images/study/{studyId}")
    @Operation(summary = "Get DICOM images for study", description = "Get all DICOM images for an imaging study")
    public ResponseEntity<List<DICOMImageResponse>> getDicomImagesByStudy(@PathVariable UUID studyId) {
        log.info("Getting DICOM images for study: {}", studyId);
        
        List<ImagingImageAttachment> attachments = dicomImageStorageService.getDicomImagesByStudy(studyId);
        List<DICOMImageResponse> responses = attachments.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/images/{attachmentId}/thumbnail")
    @Operation(summary = "Get DICOM thumbnail", description = "Get thumbnail image for a DICOM file")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID attachmentId) {
        log.info("Getting thumbnail for DICOM attachment: {}", attachmentId);
        
        try {
            ImagingImageAttachment attachment = imageAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new FileNotFoundException("Attachment not found: " + attachmentId));
            
            File thumbnailFile = resolveOrRegenerateThumbnail(attachment);
            if (thumbnailFile == null || !thumbnailFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(thumbnailFile);
            MediaType mediaType = MediaTypeFactory.getMediaType(thumbnailFile.getName()).orElse(MediaType.IMAGE_PNG);
            return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + thumbnailFile.getName() + "\"")
                .body(resource);
        } catch (FileNotFoundException e) {
            log.error("Thumbnail not found: {}", attachmentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get thumbnail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== DICOM Metadata ==========
    
    @GetMapping("/images/{attachmentId}/metadata")
    @Operation(summary = "Get DICOM metadata", description = "Extract and return DICOM metadata from a file")
    public ResponseEntity<DICOMMetadataResponse> getDicomMetadata(@PathVariable UUID attachmentId) {
        log.info("Getting DICOM metadata for: {}", attachmentId);
        
        try {
            File dicomFile = dicomImageStorageService.retrieveDicomFile(attachmentId);
            DICOMMetadataService.DICOMMetadata metadata = dicomMetadataService.extractMetadata(dicomFile);
            return ResponseEntity.ok(mapMetadataToResponse(metadata));
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get DICOM metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== DICOM Compression ==========
    
    @PostMapping("/images/{attachmentId}/compress/lossless")
    @Operation(summary = "Compress DICOM file (lossless)", description = "Compress a DICOM file using lossless compression")
    public ResponseEntity<DICOMImageResponse> compressLossless(
            @PathVariable UUID attachmentId,
            @RequestParam(required = false, defaultValue = "JPEG_LS") String compressionType) {
        
        log.info("Compressing DICOM file losslessly: {}", attachmentId);
        
        try {
            File sourceFile = dicomImageStorageService.retrieveDicomFile(attachmentId);
            File targetFile = new File(sourceFile.getParent(), sourceFile.getName() + ".compressed");
            
            DICOMCompressionService.CompressionType type = DICOMCompressionService.CompressionType.valueOf(compressionType);
            DICOMCompressionService.CompressionResult result = dicomCompressionService.compressLossless(sourceFile, targetFile, type);
            
            if (result.isSuccess()) {
                // Return information about compressed file
                return ResponseEntity.ok(DICOMImageResponse.builder()
                    .attachmentId(attachmentId)
                    .compressionRatio(result.getCompressionRatio())
                    .build());
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to compress DICOM file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/images/{attachmentId}/compress/lossy")
    @Operation(summary = "Compress DICOM file (lossy)", description = "Compress a DICOM file using lossy compression")
    public ResponseEntity<DICOMImageResponse> compressLossy(
            @PathVariable UUID attachmentId,
            @RequestParam(required = false, defaultValue = "JPEG") String compressionType,
            @RequestParam(required = false, defaultValue = "80") int quality) {
        
        log.info("Compressing DICOM file with lossy compression: {}", attachmentId);
        
        try {
            File sourceFile = dicomImageStorageService.retrieveDicomFile(attachmentId);
            File targetFile = new File(sourceFile.getParent(), sourceFile.getName() + ".compressed");
            
            DICOMCompressionService.CompressionType type = DICOMCompressionService.CompressionType.valueOf(compressionType);
            DICOMCompressionService.CompressionResult result = dicomCompressionService.compressLossy(sourceFile, targetFile, type, quality);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(DICOMImageResponse.builder()
                    .attachmentId(attachmentId)
                    .compressionRatio(result.getCompressionRatio())
                    .build());
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to compress DICOM file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== DICOM Network Operations ==========

    @PostMapping("/network/c-echo")
    @Operation(summary = "C-ECHO: Verify PACS connectivity", description = "Send DICOM Verification (C-ECHO) to the remote AE")
    public ResponseEntity<DICOMNetworkService.CEchoResult> cEcho(
            @RequestParam(required = false) String remoteAeTitle,
            @RequestParam(required = false) String remoteHost,
            @RequestParam(required = false, defaultValue = "0") int remotePort) {

        log.info("C-ECHO operation");
        try {
            DICOMNetworkService.CEchoResult result = dicomNetworkService.cEcho(
                remoteAeTitle,
                remoteHost,
                remotePort > 0 ? remotePort : null);
            return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        } catch (IOException e) {
            log.warn("C-ECHO upstream failure: {}", e.getMessage());
            DICOMNetworkService.CEchoResult err = new DICOMNetworkService.CEchoResult();
            err.setSuccess(false);
            err.setStatus(-1);
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
        } catch (Exception e) {
            log.error("C-ECHO failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/network/c-store/{attachmentId}")
    @Operation(summary = "C-STORE: Store DICOM to PACS", description = "Store a DICOM file to remote PACS using C-STORE")
    public ResponseEntity<DICOMImageResponse> cStore(
            @PathVariable UUID attachmentId,
            @RequestParam(required = false) String remoteAeTitle,
            @RequestParam(required = false) String remoteHost,
            @RequestParam(required = false, defaultValue = "0") int remotePort) {
        
        log.info("C-STORE operation for attachment: {}", attachmentId);
        
        try {
            File dicomFile = dicomImageStorageService.retrieveDicomFile(attachmentId);
            DICOMNetworkService.CStoreResult result = dicomNetworkService.cStore(
                dicomFile,
                remoteAeTitle,
                remoteHost,
                remotePort > 0 ? remotePort : null);

            DICOMImageResponse response = DICOMImageResponse.builder()
                .attachmentId(attachmentId)
                .networkSuccess(result.isSuccess())
                .networkStatus(result.getStatus())
                .networkMessage(result.getMessage())
                .networkSopInstanceUID(result.getSopInstanceUID())
                .build();
            // Return payload for both success and DIMSE-level failure so callers can inspect remote status.
            return result.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("C-STORE failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/network/c-find")
    @Operation(summary = "C-FIND: Query PACS", description = "Query remote PACS for DICOM studies using C-FIND")
    public ResponseEntity<?> cFind(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String studyInstanceUID,
            @RequestParam(required = false) String accessionNumber,
            @RequestParam(required = false) String studyDate,
            @RequestParam(required = false) String modality,
            @RequestParam(required = false) String remoteAeTitle,
            @RequestParam(required = false) String remoteHost,
            @RequestParam(required = false, defaultValue = "0") int remotePort) {
        
        log.info("C-FIND operation");
        
        try {
            DICOMNetworkService.CFindQuery query = new DICOMNetworkService.CFindQuery();
            query.setPatientId(patientId);
            query.setStudyInstanceUID(studyInstanceUID);
            query.setAccessionNumber(accessionNumber);
            query.setStudyDate(studyDate);
            query.setModality(modality);
            query.setRemoteAeTitle(remoteAeTitle);
            query.setRemoteHost(remoteHost);
            query.setRemotePort(remotePort);
            
            List<DICOMNetworkService.CFindResult> results = dicomNetworkService.cFind(query);
            
            // Map results to response (simplified)
            List<DICOMImageResponse> responses = results.stream()
                .map(r -> DICOMImageResponse.builder()
                    .studyInstanceUID(r.getStudyInstanceUID())
                    .patientId(r.getPatientId())
                    .patientName(r.getPatientName())
                    .studyDate(r.getStudyDate())
                    .accessionNumber(r.getAccessionNumber())
                    .studyDescription(r.getStudyDescription())
                    .modality(r.getModality())
                    .build())
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        } catch (IOException e) {
            log.warn("C-FIND upstream failure: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Upstream C-FIND failure"));
        } catch (Exception e) {
            log.error("C-FIND failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/network/c-move")
    @Operation(summary = "C-MOVE: Retrieve to destination AE", description = "Request remote PACS to move study instances to destination AE")
    public ResponseEntity<DICOMNetworkService.CMoveResult> cMove(
            @RequestParam String studyInstanceUID,
            @RequestParam String destinationAeTitle,
            @RequestParam(required = false) String remoteAeTitle,
            @RequestParam(required = false) String remoteHost,
            @RequestParam(required = false, defaultValue = "0") int remotePort) {

        log.info("C-MOVE operation for study: {}", studyInstanceUID);
        try {
            DICOMNetworkService.CMoveRequest request = new DICOMNetworkService.CMoveRequest();
            request.setStudyInstanceUID(studyInstanceUID);
            request.setDestinationAeTitle(destinationAeTitle);
            request.setRemoteAeTitle(remoteAeTitle);
            request.setRemoteHost(remoteHost);
            request.setRemotePort(remotePort);
            DICOMNetworkService.CMoveResult result = dicomNetworkService.cMove(request);
            return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (IOException e) {
            log.warn("C-MOVE upstream failure: {}", e.getMessage());
            DICOMNetworkService.CMoveResult err = new DICOMNetworkService.CMoveResult();
            err.setSuccess(false);
            err.setStatus(-1);
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
        } catch (Exception e) {
            log.error("C-MOVE failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/network/c-get")
    @Operation(summary = "C-GET: Retrieve instances", description = "Request remote PACS C-GET for study/series")
    public ResponseEntity<DICOMNetworkService.CGetResult> cGet(
            @RequestParam String studyInstanceUID,
            @RequestParam(required = false) String seriesInstanceUID,
            @RequestParam(required = false) String remoteAeTitle,
            @RequestParam(required = false) String remoteHost,
            @RequestParam(required = false, defaultValue = "0") int remotePort) {

        log.info("C-GET operation for study: {}", studyInstanceUID);
        try {
            DICOMNetworkService.CGetRequest request = new DICOMNetworkService.CGetRequest();
            request.setStudyInstanceUID(studyInstanceUID);
            request.setSeriesInstanceUID(seriesInstanceUID);
            request.setRemoteAeTitle(remoteAeTitle);
            request.setRemoteHost(remoteHost);
            request.setRemotePort(remotePort);
            DICOMNetworkService.CGetResult result = dicomNetworkService.cGet(request);
            return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (IOException e) {
            log.warn("C-GET upstream failure: {}", e.getMessage());
            DICOMNetworkService.CGetResult err = new DICOMNetworkService.CGetResult();
            err.setSuccess(false);
            err.setStatus(-1);
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
        } catch (Exception e) {
            log.error("C-GET failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== Helper Methods ==========
    
    private DICOMImageResponse mapToResponse(ImagingImageAttachment attachment) {
        return DICOMImageResponse.builder()
            .attachmentId(attachment.getAttachmentId())
            .studyId(attachment.getStudy().getStudyId())
            .fileName(attachment.getFileName())
            .fileSize(attachment.getFileSize())
            .fileUrl(attachment.getFileUrl())
            .isDicom(attachment.getIsDicom())
            .dicomSeriesInstanceUid(attachment.getDicomSeriesInstanceUid())
            .dicomSopInstanceUid(attachment.getDicomSopInstanceUid())
            .thumbnailUrl(attachment.getThumbnailUrl())
            .uploadedDate(attachment.getUploadedDate())
            .build();
    }
    
    private DICOMMetadataResponse mapMetadataToResponse(DICOMMetadataService.DICOMMetadata metadata) {
        return DICOMMetadataResponse.builder()
            .patientId(metadata.getPatientId())
            .patientName(metadata.getPatientName())
            .patientBirthDate(metadata.getPatientBirthDate())
            .patientSex(metadata.getPatientSex())
            .studyInstanceUID(metadata.getStudyInstanceUID())
            .studyDate(metadata.getStudyDate())
            .studyTime(metadata.getStudyTime())
            .studyDescription(metadata.getStudyDescription())
            .accessionNumber(metadata.getAccessionNumber())
            .seriesInstanceUID(metadata.getSeriesInstanceUID())
            .seriesNumber(metadata.getSeriesNumber())
            .modality(metadata.getModality())
            .sopInstanceUID(metadata.getSopInstanceUID())
            .instanceNumber(metadata.getInstanceNumber())
            .rows(metadata.getRows())
            .columns(metadata.getColumns())
            .imageWidth(metadata.getColumns())
            .imageHeight(metadata.getRows())
            .windowCenter(metadata.getWindowCenter())
            .windowWidth(metadata.getWindowWidth())
            .numberOfFrames(metadata.getNumberOfFrames())
            .manufacturer(metadata.getManufacturer())
            .manufacturerModelName(metadata.getManufacturerModelName())
            .build();
    }

    private File resolveOrRegenerateThumbnail(ImagingImageAttachment attachment) {
        String existingPath = attachment.getThumbnailPath();
        if (existingPath != null && !existingPath.isBlank()) {
            File existingFile = new File(existingPath);
            if (existingFile.exists()) {
                return existingFile;
            }
        }

        try {
            File dicomFile = dicomImageStorageService.retrieveDicomFile(attachment.getAttachmentId());
            File regenerated = dicomThumbnailService.generateThumbnail(
                dicomFile,
                attachment.getStudy().getStudyId(),
                attachment.getDicomSopInstanceUid());

            if (regenerated != null && regenerated.exists()) {
                attachment.setThumbnailPath(regenerated.getAbsolutePath());
                attachment.setThumbnailUrl(dicomThumbnailService.getThumbnailUrl(attachment.getAttachmentId()));
                imageAttachmentRepository.save(attachment);
            }
            return regenerated;
        } catch (Exception e) {
            log.warn("Failed to regenerate thumbnail for attachment {}", attachment.getAttachmentId(), e);
            return null;
        }
    }
}
