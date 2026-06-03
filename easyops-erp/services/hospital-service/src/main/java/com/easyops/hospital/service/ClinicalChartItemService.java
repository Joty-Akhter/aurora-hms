package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.ClinicalChartItemRequest;
import com.easyops.hospital.dto.response.ClinicalChartItemPageResponse;
import com.easyops.hospital.dto.response.ClinicalChartItemResponse;
import com.easyops.hospital.entity.ClinicalChartItem;
import com.easyops.hospital.repository.ClinicalChartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalChartItemService {

    private final ClinicalChartItemRepository clinicalChartItemRepository;

    @Transactional(readOnly = true)
    public ClinicalChartItemPageResponse searchCatalog(String searchTerm, int page, int size, boolean investigationsOnly) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        String containsPattern = buildContainsLikePattern(normalizeSearch(searchTerm));
        Page<ClinicalChartItem> data =
                clinicalChartItemRepository.searchCatalog(containsPattern, investigationsOnly, pageable);
        return ClinicalChartItemPageResponse.builder()
                .items(data.stream().map(this::toResponse).toList())
                .page(data.getNumber())
                .size(data.getSize())
                .totalElements(data.getTotalElements())
                .totalPages(data.getTotalPages())
                .build();
    }

    /**
     * Short list for EP investigations/tests autosuggest: SubDeptName is Diagnostic, Radiology, or LabTest only (active rows).
     * Optional query matches description, P-code, dept name, sub-dept name, sub-sub-dept, and report group (same JPQL slice as repository).
     */
    @Transactional(readOnly = true)
    public List<String> autocompleteInvestigations(String query, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        String containsPattern = buildContainsLikePattern(normalizeSearch(query));
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        final int pageSize = 120;
        final int maxPages = 20;
        for (int pageIdx = 0; pageIdx < maxPages && ordered.size() < safeLimit; pageIdx++) {
            Pageable pageable = PageRequest.of(pageIdx, pageSize);
            Page<ClinicalChartItem> slice = clinicalChartItemRepository.searchInvestigations(containsPattern, pageable);
            for (ClinicalChartItem row : slice.getContent()) {
                if (row.getDescription() != null && !row.getDescription().isBlank()) {
                    ordered.add(row.getDescription().trim());
                    if (ordered.size() >= safeLimit) {
                        break;
                    }
                }
            }
            if (!slice.hasNext()) {
                break;
            }
        }
        return new ArrayList<>(ordered);
    }

    @Transactional(readOnly = true)
    public ClinicalChartItemResponse getById(UUID clinicalChartItemId) {
        ClinicalChartItem item = clinicalChartItemRepository.findById(clinicalChartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Clinical chart row not found"));
        return toResponse(item);
    }

    @Transactional
    public ClinicalChartItemResponse create(ClinicalChartItemRequest request) {
        Long legacyRowId = request.getLegacyRowId();
        if (legacyRowId == null) {
            legacyRowId = clinicalChartItemRepository.findTopByOrderByLegacyRowIdDesc()
                    .map(row -> row.getLegacyRowId() + 1)
                    .orElse(1L);
        }
        Short outTest = request.getOutTest() != null ? request.getOutTest() : Short.valueOf((short) 0);
        Short statusLegacy = request.getStatusLegacy() != null ? request.getStatusLegacy() : Short.valueOf((short) 1);

        ClinicalChartItem item = ClinicalChartItem.builder()
                .clinicalChartItemId(UUID.randomUUID())
                .legacyRowId(legacyRowId)
                .pcode(request.getPcode())
                .description(request.getDescription())
                .charge(request.getCharge())
                .deptName(request.getDeptName())
                .subDeptName(request.getSubDeptName())
                .subSubDeptName(request.getSubSubDeptName())
                .reportGroupName(request.getReportGroupName())
                .outTest(outTest)
                .statusLegacy(statusLegacy)
                .createdAt(OffsetDateTime.now())
                .build();
        return toResponse(clinicalChartItemRepository.save(item));
    }

    @Transactional
    public ClinicalChartItemResponse update(UUID clinicalChartItemId, ClinicalChartItemRequest request) {
        ClinicalChartItem item = clinicalChartItemRepository.findById(clinicalChartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Clinical chart row not found"));
        if (request.getLegacyRowId() != null) {
            item.setLegacyRowId(request.getLegacyRowId());
        }
        item.setPcode(request.getPcode());
        item.setDescription(request.getDescription());
        item.setCharge(request.getCharge());
        item.setDeptName(request.getDeptName());
        item.setSubDeptName(request.getSubDeptName());
        item.setSubSubDeptName(request.getSubSubDeptName());
        item.setReportGroupName(request.getReportGroupName());
        if (request.getOutTest() != null) {
            item.setOutTest(request.getOutTest());
        }
        if (request.getStatusLegacy() != null) {
            item.setStatusLegacy(request.getStatusLegacy());
        }
        return toResponse(clinicalChartItemRepository.save(item));
    }

    private ClinicalChartItemResponse toResponse(ClinicalChartItem c) {
        return ClinicalChartItemResponse.builder()
                .clinicalChartItemId(c.getClinicalChartItemId())
                .legacyRowId(c.getLegacyRowId())
                .pcode(c.getPcode())
                .description(c.getDescription())
                .charge(c.getCharge())
                .deptName(c.getDeptName())
                .subDeptName(c.getSubDeptName())
                .subSubDeptName(c.getSubSubDeptName())
                .reportGroupName(c.getReportGroupName())
                .outTest(c.getOutTest())
                .statusLegacy(c.getStatusLegacy())
                .build();
    }

    private static String normalizeSearch(String searchTerm) {
        return searchTerm == null ? "" : searchTerm.trim();
    }

    /**
     * JPQL LIKE pattern with escape character {@code !} (see repository ESCAPE '!').
     * Null means unrestricted search (caller matches all active rows).
     */
    static String buildContainsLikePattern(String trimmedTerm) {
        if (trimmedTerm == null) {
            return null;
        }
        String t = trimmedTerm.trim();
        if (t.isEmpty()) {
            return null;
        }
        String lower = t.toLowerCase(Locale.ROOT);
        String escapedMetas = lower.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escapedMetas + "%";
    }
}
