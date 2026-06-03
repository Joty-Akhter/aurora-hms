package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.CodeSuggestionResponse;
import com.easyops.hospital.entity.Icd10Code;
import com.easyops.hospital.entity.Icd11Code;
import com.easyops.hospital.entity.SnomedCode;
import com.easyops.hospital.repository.Icd10CodeRepository;
import com.easyops.hospital.repository.Icd11CodeRepository;
import com.easyops.hospital.repository.SnomedCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for looking up medical codes (ICD-10, ICD-11, SNOMED CT).
 * Uses the same active-code search as the medical code catalog (master data).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CodeLookupService {

    private final Icd10CodeRepository icd10CodeRepository;
    private final Icd11CodeRepository icd11CodeRepository;
    private final SnomedCodeRepository snomedCodeRepository;

    public List<CodeSuggestionResponse> searchIcd10(String searchTerm, int limit) {
        log.debug("Searching ICD-10 codes for: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        try {
            Pageable pageable = PageRequest.of(0, Math.max(1, limit));
            Page<Icd10Code> page = icd10CodeRepository.findActiveCodesBySearch(searchTerm.trim(), pageable);
            return page.getContent().stream()
                .map(code -> CodeSuggestionResponse.builder()
                    .code(code.getCode())
                    .description(code.getDescription())
                    .codeType("ICD10")
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching ICD-10 codes", e);
            return List.of();
        }
    }

    public List<CodeSuggestionResponse> searchIcd11(String searchTerm, int limit) {
        log.debug("Searching ICD-11 codes for: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        try {
            Pageable pageable = PageRequest.of(0, Math.max(1, limit));
            Page<Icd11Code> page = icd11CodeRepository.findActiveCodesBySearch(searchTerm.trim(), pageable);
            return page.getContent().stream()
                .map(code -> CodeSuggestionResponse.builder()
                    .code(code.getCode())
                    .description(code.getDescription())
                    .codeType("ICD11")
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching ICD-11 codes", e);
            return List.of();
        }
    }

    public List<CodeSuggestionResponse> searchSnomed(String searchTerm, int limit) {
        log.debug("Searching SNOMED CT codes for: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        try {
            return snomedCodeRepository.searchCodes(searchTerm.trim()).stream()
                .limit(Math.max(1, limit))
                .map(code -> CodeSuggestionResponse.builder()
                    .code(code.getCode())
                    .description(code.getDescription())
                    .codeType("SNOMED")
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching SNOMED CT codes", e);
            return List.of();
        }
    }
}
