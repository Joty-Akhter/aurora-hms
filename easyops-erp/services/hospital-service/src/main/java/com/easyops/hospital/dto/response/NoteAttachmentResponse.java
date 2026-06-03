package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.NoteAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteAttachmentResponse {
    
    private UUID attachmentId;
    private UUID noteId;
    
    // File Information
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String filePath;
    private String fileHash;
    private String mimeType;
    
    // Metadata
    private String description;
    private NoteAttachment.AttachmentType attachmentType;
    private LocalDateTime uploadedDate;
    private UUID uploadedBy;
    
    // Status
    private Boolean isActive;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
