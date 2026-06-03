package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.MedicalCodeUpsertRequest;
import com.easyops.hospital.dto.response.MedicalCodePageResponse;
import com.easyops.hospital.dto.response.MedicalCodeResponse;
import com.easyops.hospital.entity.Icd10Code;
import com.easyops.hospital.entity.Icd11Code;
import com.easyops.hospital.repository.Icd10CodeRepository;
import com.easyops.hospital.repository.Icd11CodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicalCodeManagementService {

    private final Icd10CodeRepository icd10CodeRepository;
    private final Icd11CodeRepository icd11CodeRepository;

    @Transactional(readOnly = true)
    public MedicalCodePageResponse getIcd10Codes(String searchTerm, int page, int size, boolean includeInactive) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Icd10Code> data = hasSearch(searchTerm)
            ? icd10CodeRepository.findCodesBySearch(searchTerm.trim(), includeInactive, pageable)
            : icd10CodeRepository.findCodes(includeInactive, pageable);
        return MedicalCodePageResponse.builder()
            .items(data.stream().map(this::toIcd10Response).toList())
            .page(data.getNumber())
            .size(data.getSize())
            .totalElements(data.getTotalElements())
            .totalPages(data.getTotalPages())
            .build();
    }

    @Transactional(readOnly = true)
    public MedicalCodePageResponse getIcd11Codes(String searchTerm, int page, int size, boolean includeInactive) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Icd11Code> data = hasSearch(searchTerm)
            ? icd11CodeRepository.findCodesBySearch(searchTerm.trim(), includeInactive, pageable)
            : icd11CodeRepository.findCodes(includeInactive, pageable);
        return MedicalCodePageResponse.builder()
            .items(data.stream().map(this::toIcd11Response).toList())
            .page(data.getNumber())
            .size(data.getSize())
            .totalElements(data.getTotalElements())
            .totalPages(data.getTotalPages())
            .build();
    }

    @Transactional
    public MedicalCodeResponse upsertIcd10Code(MedicalCodeUpsertRequest request) {
        String code = normalizeIcd10Code(request.getCode());
        Icd10Code codeRow = icd10CodeRepository.findById(code).orElse(Icd10Code.builder().code(code).build());
        applyToIcd10(codeRow, request);
        return toIcd10Response(icd10CodeRepository.save(codeRow));
    }

    @Transactional
    public MedicalCodeResponse upsertIcd11Code(MedicalCodeUpsertRequest request) {
        String code = normalizeIcd11Code(request.getCode());
        Icd11Code codeRow = icd11CodeRepository.findById(code).orElse(Icd11Code.builder().code(code).build());
        applyToIcd11(codeRow, request);
        return toIcd11Response(icd11CodeRepository.save(codeRow));
    }

    @Transactional
    public void deactivateIcd10Code(String code) {
        Icd10Code codeRow = icd10CodeRepository.findById(normalizeIcd10Code(code))
            .orElseThrow(() -> new IllegalArgumentException("ICD-10 code not found: " + code));
        codeRow.setIsValid(false);
        icd10CodeRepository.save(codeRow);
    }

    @Transactional
    public void deactivateIcd11Code(String code) {
        Icd11Code codeRow = icd11CodeRepository.findById(normalizeIcd11Code(code))
            .orElseThrow(() -> new IllegalArgumentException("ICD-11 code not found: " + code));
        codeRow.setIsValid(false);
        icd11CodeRepository.save(codeRow);
    }

    private static final java.util.regex.Pattern ICD10_PATTERN =
        java.util.regex.Pattern.compile("^[A-TV-Z][0-9][0-9AB]([A-Z0-9]{0,4})?$");
    private static final java.util.regex.Pattern ICD11_PATTERN =
        java.util.regex.Pattern.compile("^[0-9A-HJ-NP-Z]{4}(\\.[0-9A-HJ-NP-Z]{1,4})*$");

    private boolean hasSearch(String searchTerm) {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }

    private void applyToIcd10(Icd10Code row, MedicalCodeUpsertRequest request) {
        row.setDescription(normalizeText(request.getDescription()));
        row.setCategory(normalizeNullableText(request.getCategory()));
        row.setChapter(normalizeNullableText(request.getChapter()));
        row.setIsValid(request.getIsValid() != null ? request.getIsValid() : true);
    }

    private void applyToIcd11(Icd11Code row, MedicalCodeUpsertRequest request) {
        row.setDescription(normalizeText(request.getDescription()));
        row.setCategory(normalizeNullableText(request.getCategory()));
        row.setChapter(normalizeNullableText(request.getChapter()));
        row.setIsValid(request.getIsValid() != null ? request.getIsValid() : true);
    }

    private String normalizeIcd10Code(String value) {
        String code = normalizeCode(value);
        if (code.length() < 3 || code.length() > 7) {
            throw new IllegalArgumentException("ICD-10 code must be 3 to 7 characters (e.g. A00, E11.9).");
        }
        if (!ICD10_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Invalid ICD-10 format. Use letter + digits (e.g. A00, J06.9, E11.65).");
        }
        return code;
    }

    private String normalizeIcd11Code(String value) {
        String code = normalizeCode(value);
        if (code.length() < 4) {
            throw new IllegalArgumentException("ICD-11 code must be at least 4 characters (e.g. 1A00).");
        }
        if (!ICD11_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Invalid ICD-11 format. Use alphanumeric codes (e.g. 1A00, 5A11.0).");
        }
        return code;
    }

    private String normalizeCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Code is required");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MedicalCodeResponse toIcd10Response(Icd10Code row) {
        return MedicalCodeResponse.builder()
            .code(row.getCode())
            .description(row.getDescription())
            .category(row.getCategory())
            .chapter(row.getChapter())
            .isValid(row.getIsValid())
            .codeType("ICD10")
            .build();
    }

    private MedicalCodeResponse toIcd11Response(Icd11Code row) {
        return MedicalCodeResponse.builder()
            .code(row.getCode())
            .description(row.getDescription())
            .category(row.getCategory())
            .chapter(row.getChapter())
            .isValid(row.getIsValid())
            .codeType("ICD11")
            .build();
    }
}
