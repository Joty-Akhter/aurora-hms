package com.easyops.hospital.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/**
 * Generic document storage service for EHR patient documents.
 * Stores files under {base-path}/{patientId}/{YYYY-MM}/{documentId}.{ext}
 */
@Service
@Slf4j
public class DocumentStorageService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    // Allowed MIME types — covers all expected EHR document categories
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/pdf",
        "image/jpeg", "image/jpg", "image/png", "image/tiff", "image/bmp", "image/webp",
        "text/plain", "text/csv",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "audio/mpeg", "audio/wav", "audio/ogg",
        "video/mp4", "video/quicktime"
    );

    @Value("${ehr.document.storage.base-path:./storage/documents}")
    private String baseStoragePath;

    /**
     * Store a file for a patient and return stored file info.
     *
     * @return StoredFile record containing path, URL token, hash, etc.
     */
    public StoredFile store(UUID patientId, UUID documentId, MultipartFile file) throws IOException {
        validateFile(file);

        byte[] content = file.getBytes();
        String hash = computeSha256(content);

        String ext = extractExtension(file.getOriginalFilename(), file.getContentType());
        String storedName = documentId.toString() + (ext.isEmpty() ? "" : "." + ext);

        // Shard by patient + year-month to avoid huge flat directories
        String yearMonth = LocalDate.now().toString().substring(0, 7); // "2026-04"
        Path dir = Paths.get(baseStoragePath, patientId.toString(), yearMonth);
        Files.createDirectories(dir);

        Path target = dir.resolve(storedName);
        Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Stored document {} for patient {} at {}", documentId, patientId, target);

        return new StoredFile(
            storedName,
            target.toString(),
            "/api/hospital/documents/" + documentId + "/download",
            file.getSize(),
            file.getContentType(),
            hash
        );
    }

    /**
     * Retrieve a stored file path for download.
     */
    public Path retrieve(String filePath) throws FileNotFoundException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new FileNotFoundException("Document file not found: " + filePath);
        }
        return path;
    }

    /**
     * Delete a stored file.
     */
    public void delete(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
            log.info("Deleted document file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete document file {}: {}", filePath, e.getMessage());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum allowed size of 50 MB");
        }
        String mime = file.getContentType();
        if (mime != null && !ALLOWED_MIME_TYPES.contains(mime.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed: " + mime);
        }
    }

    private String extractExtension(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        if (contentType == null) return "";
        return switch (contentType.toLowerCase()) {
            case "application/pdf" -> "pdf";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/tiff" -> "tif";
            case "text/plain" -> "txt";
            case "text/csv" -> "csv";
            default -> "";
        };
    }

    private String computeSha256(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(content));
        } catch (Exception e) {
            return null;
        }
    }

    public record StoredFile(
        String fileName,
        String filePath,
        String fileUrl,
        long fileSize,
        String mimeType,
        String fileHash
    ) {}
}
