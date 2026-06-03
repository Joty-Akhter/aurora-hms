package com.easyops.hospital.service;

import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.image.BufferedImageUtils;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReader;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Generates DICOM thumbnails and previews by decoding pixel data via dcm4che {@link DicomImageReader}.
 */
@Service
@Slf4j
public class DICOMThumbnailService {

    static {
        ImageIO.scanForPlugins();
    }

    @Value("${dicom.thumbnail.enabled:true}")
    private boolean thumbnailEnabled;

    @Value("${dicom.thumbnail.base-path:./storage/dicom/thumbnails}")
    private String thumbnailBasePath;

    @Value("${dicom.thumbnail.width:256}")
    private int thumbnailWidth;

    @Value("${dicom.thumbnail.height:256}")
    private int thumbnailHeight;

    @Value("${dicom.thumbnail.format:PNG}")
    private String thumbnailFormat;

    public File generateThumbnail(File dicomFile, UUID studyId, String sopInstanceUid) throws IOException {
        if (!thumbnailEnabled) {
            log.debug("Thumbnail generation is disabled");
            return null;
        }
        if (studyId == null) {
            throw new IllegalArgumentException("studyId is required for thumbnail output path");
        }
        if (dicomFile == null || !dicomFile.isFile() || !dicomFile.canRead()) {
            log.warn("Invalid or unreadable DICOM file for thumbnail");
            return null;
        }

        log.info("Generating thumbnail for DICOM file: {}", dicomFile.getName());

        BufferedImage image = extractImageFromDicom(dicomFile);
        if (image == null) {
            log.warn("Could not extract image from DICOM file: {}", dicomFile.getName());
            return null;
        }

        BufferedImage thumbnail = Scalr.resize(
            image,
            Scalr.Method.QUALITY,
            Scalr.Mode.AUTOMATIC,
            thumbnailWidth,
            thumbnailHeight);

        String thumbnailFileName = generateThumbnailFileName(studyId, sopInstanceUid);
        Path thumbnailPath = Paths.get(thumbnailBasePath, studyId.toString());
        Files.createDirectories(thumbnailPath);

        File thumbnailFile = thumbnailPath.resolve(thumbnailFileName).toFile();
        String outputFormat = imageWriterFormatName();
        boolean written = ImageIO.write(thumbnail, outputFormat, thumbnailFile);
        if (!written) {
            throw new IOException("Unsupported thumbnail image format: " + outputFormat);
        }

        log.info("Generated thumbnail: {}", thumbnailFile.getAbsolutePath());
        return thumbnailFile;
    }

    public File generatePreview(File dicomFile, UUID studyId, String sopInstanceUid, int maxWidth, int maxHeight) throws IOException {
        if (!thumbnailEnabled) {
            log.debug("Preview generation is disabled");
            return null;
        }
        if (studyId == null) {
            throw new IllegalArgumentException("studyId is required for preview output path");
        }
        if (dicomFile == null || !dicomFile.isFile() || !dicomFile.canRead()) {
            log.warn("Invalid or unreadable DICOM file for preview");
            return null;
        }

        log.info("Generating preview for DICOM file: {}", dicomFile.getName());

        BufferedImage image = extractImageFromDicom(dicomFile);
        if (image == null) {
            log.warn("Could not extract image from DICOM file: {}", dicomFile.getName());
            return null;
        }

        BufferedImage preview = Scalr.resize(
            image,
            Scalr.Method.QUALITY,
            Scalr.Mode.AUTOMATIC,
            maxWidth,
            maxHeight);

        String previewFileName = generatePreviewFileName(studyId, sopInstanceUid);
        Path previewPath = Paths.get(thumbnailBasePath, studyId.toString(), "previews");
        Files.createDirectories(previewPath);

        File previewFile = previewPath.resolve(previewFileName).toFile();
        String outputFormat = imageWriterFormatName();
        boolean written = ImageIO.write(preview, outputFormat, previewFile);
        if (!written) {
            throw new IOException("Unsupported preview image format: " + outputFormat);
        }

        log.info("Generated preview: {}", previewFile.getAbsolutePath());
        return previewFile;
    }

    /**
     * Decode the first frame to a displayable RGB image using DICOM Image I/O.
     */
    private BufferedImage extractImageFromDicom(File dicomFile) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(dicomFile);
        if (iis == null) {
            log.warn("Could not open ImageInputStream for {}", dicomFile.getName());
            return null;
        }
        try (iis; DicomImageReader reader = new DicomImageReader(new DicomImageReaderSpi())) {
            reader.setInput(iis, false, true);
            BufferedImage bi = reader.read(0, reader.getDefaultReadParam());
            if (bi == null) {
                return null;
            }
            return BufferedImageUtils.convertToIntRGB(bi);
        }
    }

    private String generateThumbnailFileName(UUID studyId, String sopInstanceUid) {
        String ext = normalizedFormat().toLowerCase();
        if (sopInstanceUid != null) {
            return sopInstanceUid.replace(".", "_") + "_thumb." + ext;
        }
        return studyId.toString() + "_" + System.currentTimeMillis() + "_thumb." + ext;
    }

    private String generatePreviewFileName(UUID studyId, String sopInstanceUid) {
        String ext = normalizedFormat().toLowerCase();
        if (sopInstanceUid != null) {
            return sopInstanceUid.replace(".", "_") + "_preview." + ext;
        }
        return studyId.toString() + "_" + System.currentTimeMillis() + "_preview." + ext;
    }

    private String normalizedFormat() {
        if (thumbnailFormat == null || thumbnailFormat.isBlank()) {
            return "PNG";
        }
        return thumbnailFormat.trim();
    }

    /** ImageIO writer names (e.g. {@code jpeg}, not {@code jpg}). */
    private String imageWriterFormatName() {
        String f = normalizedFormat().toLowerCase();
        if (f.isEmpty()) {
            return "png";
        }
        if ("jpg".equals(f)) {
            return "jpeg";
        }
        return f;
    }

    public void deleteThumbnail(String thumbnailPath) throws IOException {
        if (thumbnailPath != null) {
            Path path = Paths.get(thumbnailPath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Deleted thumbnail: {}", thumbnailPath);
            }
        }
    }

    public String getThumbnailUrl(UUID attachmentId) {
        return "/api/hospital/dicom/images/" + attachmentId + "/thumbnail";
    }
}
