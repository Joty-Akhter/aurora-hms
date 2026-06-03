package com.easyops.hospital.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "note_attachments", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = {"note"})
public class NoteAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "attachment_id")
    private UUID attachmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private ClinicalNote note;
    
    // File Information
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "file_type", length = 100)
    private String fileType;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    @Column(name = "file_hash", length = 255)
    private String fileHash;
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    // Metadata
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "attachment_type", length = 50)
    @Enumerated(EnumType.STRING)
    private AttachmentType attachmentType;
    
    @Column(name = "uploaded_date")
    @Builder.Default
    private LocalDateTime uploadedDate = LocalDateTime.now();
    
    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;
    
    // Status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum AttachmentType {
        IMAGE, DOCUMENT, LAB_RESULT, IMAGING, AUDIO, VIDEO, OTHER
    }
}
