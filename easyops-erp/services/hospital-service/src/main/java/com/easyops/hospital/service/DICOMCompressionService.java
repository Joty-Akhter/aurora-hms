package com.easyops.hospital.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.UID;
import org.dcm4che3.imageio.codec.Transcoder;
import org.dcm4che3.util.Property;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * DICOM image compression (lossless and lossy) and decompression using dcm4che {@link Transcoder}.
 */
@Service
@Slf4j
public class DICOMCompressionService {

    /**
     * Compress DICOM file with lossless compression.
     */
    public CompressionResult compressLossless(File sourceFile, File targetFile, CompressionType compressionType) throws IOException {
        validateDicomFiles(sourceFile, targetFile);
        log.info("Compressing DICOM file losslessly: {}", sourceFile.getName());
        String destTs = getLosslessCompressionUID(compressionType);
        try {
            transcodeTo(sourceFile, targetFile, destTs);
            return buildResult(sourceFile, targetFile, compressionType, true, null);
        } catch (Exception e) {
            log.error("Lossless compression failed", e);
            throw new IOException("Lossless compression failed: " + e.getMessage(), e);
        }
    }

    /**
     * Compress DICOM file with lossy compression.
     */
    public CompressionResult compressLossy(File sourceFile, File targetFile, CompressionType compressionType, int quality) throws IOException {
        validateDicomFiles(sourceFile, targetFile);
        log.info("Compressing DICOM file with lossy compression: {}", sourceFile.getName());

        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException("Quality must be between 1 and 100");
        }

        String destTs = getLossyCompressionUID(compressionType, quality);
        float compressionQuality = quality / 100f;
        try {
            transcodeTo(sourceFile, targetFile, destTs, new Property("compressionQuality", compressionQuality));
            return buildResult(sourceFile, targetFile, compressionType, false, quality);
        } catch (Exception e) {
            log.error("Lossy compression failed", e);
            throw new IOException("Lossy compression failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decompress DICOM pixel data to uncompressed Explicit VR Little Endian.
     */
    public void decompress(File sourceFile, File targetFile) throws IOException {
        validateDicomFiles(sourceFile, targetFile);
        log.info("Decompressing DICOM file: {}", sourceFile.getName());
        try {
            transcodeTo(sourceFile, targetFile, UID.ExplicitVRLittleEndian);
            log.info("Decompression completed");
        } catch (Exception e) {
            log.error("Decompression failed", e);
            throw new IOException("Decompression failed: " + e.getMessage(), e);
        }
    }

    private static void validateDicomFiles(File sourceFile, File targetFile) throws IOException {
        if (sourceFile == null || !sourceFile.isFile() || !sourceFile.canRead()) {
            throw new IOException("Source DICOM file is missing or not readable");
        }
        if (targetFile == null) {
            throw new IOException("Target file path is required");
        }
        Path parent = targetFile.toPath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void transcodeTo(File sourceFile, File targetFile, String destinationTransferSyntax) throws IOException {
        transcodeTo(sourceFile, targetFile, destinationTransferSyntax, new Property[0]);
    }

    private static void transcodeTo(File sourceFile, File targetFile, String destinationTransferSyntax, Property... compressParams)
        throws IOException {
        try (Transcoder transcoder = new Transcoder(sourceFile)) {
            transcoder.setDestinationTransferSyntax(destinationTransferSyntax);
            transcoder.setIncludeFileMetaInformation(true);
            if (compressParams != null && compressParams.length > 0) {
                transcoder.setCompressParams(compressParams);
            }
            transcoder.transcode((t, dataset) -> new FileOutputStream(targetFile));
        }
    }

    private static CompressionResult buildResult(
        File sourceFile,
        File targetFile,
        CompressionType compressionType,
        boolean lossless,
        Integer quality
    ) {
        CompressionResult result = new CompressionResult();
        result.setSuccess(true);
        result.setOriginalSize(sourceFile.length());
        result.setCompressedSize(targetFile.length());
        long orig = result.getOriginalSize();
        if (orig > 0) {
            result.setCompressionRatio((double) result.getCompressedSize() / orig);
        } else {
            result.setCompressionRatio(1.0);
        }
        result.setCompressionType(compressionType);
        result.setLossless(lossless);
        result.setQuality(quality);
        log.info(
            "Compression completed. Compressed/original ratio: {} ({}%)",
            String.format("%.4f", result.getCompressionRatio()),
            String.format("%.2f", result.getCompressionRatio() * 100));
        return result;
    }

    private String getLosslessCompressionUID(CompressionType type) {
        return switch (type) {
            case JPEG_LS -> UID.JPEGLSLossless;
            case JPEG_2000 -> UID.JPEG2000Lossless;
            case RLE -> UID.RLELossless;
            default -> UID.JPEGLSLossless;
        };
    }

    private String getLossyCompressionUID(CompressionType type, int quality) {
        return switch (type) {
            case JPEG -> UID.JPEGBaseline8Bit;
            case JPEG_2000 -> UID.JPEG2000;
            case JPEG_LS -> UID.JPEGLSNearLossless;
            case RLE -> UID.RLELossless;
        };
    }

    public enum CompressionType {
        JPEG,
        JPEG_LS,
        JPEG_2000,
        RLE
    }

    @Data
    public static class CompressionResult {
        private boolean success;
        private long originalSize;
        private long compressedSize;
        private double compressionRatio;
        private CompressionType compressionType;
        private boolean lossless;
        private Integer quality;
    }
}
