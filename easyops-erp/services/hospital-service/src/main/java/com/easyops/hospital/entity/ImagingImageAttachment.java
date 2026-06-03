package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "imaging_image_attachments", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ImagingImageAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "attachment_id")
    private UUID attachmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private ImagingStudy study;
    
    // File Information
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    @Column(name = "file_url", length = 500)
    private String fileUrl;
    
    // Image Information
    @Column(name = "image_type", length = 50)
    @Enumerated(EnumType.STRING)
    private ImageType imageType;
    
    @Column(name = "is_dicom")
    private Boolean isDicom = false;
    
    @Column(name = "dicom_series_instance_uid", length = 200)
    private String dicomSeriesInstanceUid;
    
    @Column(name = "dicom_sop_instance_uid", length = 200)
    private String dicomSopInstanceUid;
    
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;
    
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    // Metadata
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;
    
    @Column(name = "uploaded_date", nullable = false)
    private LocalDateTime uploadedDate;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ImageType {
        DICOM, JPG, PNG, TIFF, PDF, OTHER
    }
}
