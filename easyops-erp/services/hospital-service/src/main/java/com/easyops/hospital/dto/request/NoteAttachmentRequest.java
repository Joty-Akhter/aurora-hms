package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.NoteAttachment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteAttachmentRequest {
    
    @NotBlank(message = "File name is required")
    private String fileName;
    
    private String fileType;
    
    private Long fileSize;
    
    @NotBlank(message = "File path is required")
    private String filePath;
    
    private String fileHash;
    
    private String mimeType;
    
    private String description;
    
    private NoteAttachment.AttachmentType attachmentType;
}
