package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.PatientDocumentResponse;
import com.easyops.hospital.entity.PatientDocument;
import com.easyops.hospital.repository.PatientDocumentRepository;
import com.easyops.hospital.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientDocumentService {

    private final PatientDocumentRepository documentRepository;
    private final PatientRepository patientRepository;
    private final DocumentStorageService storageService;

    @Transactional
    public PatientDocumentResponse uploadDocument(
            UUID patientId,
            MultipartFile file,
            PatientDocument.DocumentType documentType,
            String title,
            String description,
            String sourceFacility,
            LocalDateTime documentDate,
            UUID encounterId,
            UUID clinicalNoteId,
            UUID labResultId,
            UUID prescriptionId,
            Boolean isConfidential,
            UUID uploadedBy) throws IOException {

        patientRepository.findById(patientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found: " + patientId));

        PatientDocument.DocumentType effectiveDocumentType =
            documentType != null ? documentType : PatientDocument.DocumentType.OTHER;
        if (effectiveDocumentType == PatientDocument.DocumentType.OTHER
                && (description == null || description.trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Description is required when document type is OTHER");
        }

        UUID documentId = UUID.randomUUID();
        final DocumentStorageService.StoredFile stored;
        try {
            stored = storageService.store(patientId, documentId, file);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to store uploaded document", ex);
        }

        PatientDocument doc = PatientDocument.builder()
            .documentId(documentId)
            .patientId(patientId)
            .encounterId(encounterId)
            .clinicalNoteId(clinicalNoteId)
            .labResultId(labResultId)
            .prescriptionId(prescriptionId)
            .documentType(effectiveDocumentType)
            .title(title != null ? title : file.getOriginalFilename())
            .description(description)
            .fileName(stored.fileName())
            .originalFileName(file.getOriginalFilename())
            .filePath(stored.filePath())
            .fileUrl(stored.fileUrl())
            .fileSize(stored.fileSize())
            .mimeType(stored.mimeType())
            .fileHash(stored.fileHash())
            .sourceFacility(sourceFacility)
            .documentDate(documentDate)
            .uploadedBy(uploadedBy)
            .uploadedDate(LocalDateTime.now())
            .isActive(true)
            .isConfidential(isConfidential != null && isConfidential)
            .build();

        PatientDocument saved = documentRepository.save(doc);
        log.info("Uploaded document {} ({}) for patient {}", saved.getDocumentId(), saved.getDocumentType(), patientId);
        return toResponse(saved);
    }

    public List<PatientDocumentResponse> listDocuments(UUID patientId) {
        patientRepository.findById(patientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found: " + patientId));
        return documentRepository.findActiveByPatientId(patientId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<PatientDocumentResponse> listDocumentsByType(UUID patientId, PatientDocument.DocumentType type) {
        return documentRepository.findActiveByPatientIdAndType(patientId, type).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<PatientDocumentResponse> listDocumentsByNote(UUID noteId) {
        return documentRepository.findActiveByNoteId(noteId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<PatientDocumentResponse> listDocumentsByLabResult(UUID labResultId) {
        return documentRepository.findActiveByLabResultId(labResultId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<PatientDocumentResponse> listDocumentsByPrescription(UUID prescriptionId) {
        return documentRepository.findActiveByPrescriptionId(prescriptionId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public Resource downloadDocument(UUID documentId) throws IOException {
        PatientDocument doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));
        if (!Boolean.TRUE.equals(doc.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Document has been deleted");
        }
        return new PathResource(storageService.retrieve(doc.getFilePath()));
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        PatientDocument doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));
        doc.setIsActive(false);
        documentRepository.save(doc);
        storageService.delete(doc.getFilePath());
        log.info("Deleted document {}", documentId);
    }

    private PatientDocumentResponse toResponse(PatientDocument doc) {
        return PatientDocumentResponse.builder()
            .documentId(doc.getDocumentId())
            .patientId(doc.getPatientId())
            .organizationId(doc.getOrganizationId())
            .encounterId(doc.getEncounterId())
            .clinicalNoteId(doc.getClinicalNoteId())
            .labResultId(doc.getLabResultId())
            .prescriptionId(doc.getPrescriptionId())
            .documentType(doc.getDocumentType())
            .documentCategory(doc.getDocumentCategory())
            .title(doc.getTitle())
            .description(doc.getDescription())
            .fileName(doc.getFileName())
            .originalFileName(doc.getOriginalFileName())
            .fileUrl(doc.getFileUrl())
            .fileSize(doc.getFileSize())
            .mimeType(doc.getMimeType())
            .sourceFacility(doc.getSourceFacility())
            .documentDate(doc.getDocumentDate())
            .uploadedBy(doc.getUploadedBy())
            .uploadedDate(doc.getUploadedDate())
            .isActive(doc.getIsActive())
            .isConfidential(doc.getIsConfidential())
            .createdAt(doc.getCreatedAt())
            .updatedAt(doc.getUpdatedAt())
            .build();
    }
}
