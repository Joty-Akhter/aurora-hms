package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.DrugRequest;
import com.easyops.hospitalpharmacy.dto.request.FormularyRuleRequest;
import com.easyops.hospitalpharmacy.dto.response.DrugResponse;
import com.easyops.hospitalpharmacy.dto.response.FormularyRuleResponse;
import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.FormularyRule;
import com.easyops.hospitalpharmacy.entity.Manufacturer;
import com.easyops.hospitalpharmacy.entity.ProductGroup;
import com.easyops.hospitalpharmacy.entity.Unit;
import com.easyops.hospitalpharmacy.repository.DrugRepository;
import com.easyops.hospitalpharmacy.repository.FormularyRuleRepository;
import com.easyops.hospitalpharmacy.repository.ManufacturerRepository;
import com.easyops.hospitalpharmacy.repository.ProductGroupRepository;
import com.easyops.hospitalpharmacy.repository.UnitRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugRepository drugRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final FormularyRuleRepository formularyRuleRepository;
    private final ProductGroupRepository productGroupRepository;
    private final UnitRepository unitRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DrugResponse create(DrugRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + request.getManufacturerId()));

        ProductGroup productGroup = null;
        if (request.getProductGroupId() != null) {
            productGroup = productGroupRepository.findById(request.getProductGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("ProductGroup not found: " + request.getProductGroupId()));
        }

        Unit dispensingUnit = null;
        if (request.getDispensingUnitId() != null) {
            dispensingUnit = unitRepository.findById(request.getDispensingUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + request.getDispensingUnitId()));
        }

        Drug drug = Drug.builder()
                .genericName(request.getGenericName())
                .brandName(request.getBrandName())
                .strength(request.getStrength())
                .form(request.getForm())
                .route(request.getRoute())
                .packSize(request.getPackSize())
                .unitOfMeasure(request.getUnitOfMeasure())
                .therapeuticClassId(request.getTherapeuticClassId())
                .active(request.getActive() == null || request.getActive())
                .controlledDrugFlag(request.getControlledDrugFlag() != null && request.getControlledDrugFlag())
                .controlledProfileCode(normalizeProfileCode(request.getControlledProfileCode()))
                .batchRequired(request.getBatchRequired() == null || request.getBatchRequired())
                .expiryRequired(request.getExpiryRequired() == null || request.getExpiryRequired())
                .manufacturer(manufacturer)
                .productGroup(productGroup)
                .dispensingUnit(dispensingUnit)
                .mrp(request.getMrp())
                .salePrice(request.getSalePrice())
                .purchasePrice(request.getPurchasePrice())
                .rackNo(request.getRackNo())
                .reminderStock(request.getReminderStock() != null ? request.getReminderStock() : BigDecimal.ZERO)
                .hsnCode(request.getHsnCode())
                .productCode(request.getProductCode())
                .departmentId(request.getDepartmentId())
                .build();
        Drug saved = drugRepository.save(drug);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DrugResponse getById(UUID id) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + id));
        return toResponse(drug);
    }

    @Transactional(readOnly = true)
    public List<DrugResponse> search(String name, Boolean activeOnly, UUID manufacturerId) {
        List<Drug> drugs;
        if (name != null && !name.isBlank()) {
            drugs = drugRepository.findByGenericNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(name, name);
            if (Boolean.TRUE.equals(activeOnly)) {
                drugs = drugs.stream().filter(Drug::isActive).toList();
            }
        } else if (Boolean.TRUE.equals(activeOnly)) {
            drugs = drugRepository.findByActiveIsTrue();
        } else {
            drugs = drugRepository.findAll();
        }

        if (manufacturerId != null) {
            Manufacturer manufacturer = manufacturerRepository.findById(manufacturerId)
                    .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + manufacturerId));
            drugs = drugs.stream()
                    .filter(d -> d.getManufacturer().equals(manufacturer))
                    .toList();
        }

        return drugs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Prescription autocomplete: active drugs matching generic or brand name.
     * Order: (1) brand name match quality, (2) generic name match quality.
     * Match quality per field: exact match, then prefix, then substring.
     * Rows that match on brand are listed before rows that match only on generic.
     */
    @Transactional(readOnly = true)
    public Page<DrugResponse> searchForPrescription(String query, int page, int size) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }
        String qNorm = q.toLowerCase(Locale.ROOT);
        List<Drug> matches = drugRepository.findActiveByGenericOrBrandNameContaining(q);
        List<Drug> sorted = matches.stream()
                .sorted(prescriptionDrugComparator(qNorm))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size);
        int total = sorted.size();
        int start = (int) pageable.getOffset();
        if (start >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }
        int end = Math.min(start + pageable.getPageSize(), total);
        List<DrugResponse> content = sorted.subList(start, end).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }

    private static final int RANK_EXACT = 0;
    private static final int RANK_STARTS = 1;
    private static final int RANK_CONTAINS = 2;
    private static final int RANK_NO = 3;

    private static int nameMatchRank(String value, String qNorm) {
        if (value == null || value.isBlank()) {
            return RANK_NO;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.equals(qNorm)) {
            return RANK_EXACT;
        }
        if (v.startsWith(qNorm)) {
            return RANK_STARTS;
        }
        if (v.contains(qNorm)) {
            return RANK_CONTAINS;
        }
        return RANK_NO;
    }

    private static Comparator<Drug> prescriptionDrugComparator(String qNorm) {
        return Comparator
                .comparing((Drug d) -> nameMatchRank(d.getBrandName(), qNorm))
                .thenComparing(d -> nameMatchRank(d.getGenericName(), qNorm))
                .thenComparing(Drug::getId);
    }

    @Transactional
    public DrugResponse update(UUID id, DrugRequest request) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + id));

        if (request.getGenericName() != null) {
            drug.setGenericName(request.getGenericName());
        }
        if (request.getBrandName() != null) {
            drug.setBrandName(request.getBrandName());
        }
        if (request.getStrength() != null) {
            drug.setStrength(request.getStrength());
        }
        if (request.getForm() != null) {
            drug.setForm(request.getForm());
        }
        if (request.getRoute() != null) {
            drug.setRoute(request.getRoute());
        }
        if (request.getPackSize() != null) {
            drug.setPackSize(request.getPackSize());
        }
        if (request.getUnitOfMeasure() != null) {
            drug.setUnitOfMeasure(request.getUnitOfMeasure());
        }
        if (request.getTherapeuticClassId() != null) {
            drug.setTherapeuticClassId(request.getTherapeuticClassId());
        }
        if (request.getActive() != null) {
            drug.setActive(request.getActive());
        }
        if (request.getControlledDrugFlag() != null) {
            drug.setControlledDrugFlag(request.getControlledDrugFlag());
        }
        if (request.getControlledProfileCode() != null) {
            drug.setControlledProfileCode(normalizeProfileCode(request.getControlledProfileCode()));
        }
        if (request.getBatchRequired() != null) {
            drug.setBatchRequired(request.getBatchRequired());
        }
        if (request.getExpiryRequired() != null) {
            drug.setExpiryRequired(request.getExpiryRequired());
        }
        if (request.getManufacturerId() != null) {
            Manufacturer manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                    .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + request.getManufacturerId()));
            drug.setManufacturer(manufacturer);
        }
        if (request.getProductGroupId() != null) {
            ProductGroup productGroup = productGroupRepository.findById(request.getProductGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("ProductGroup not found: " + request.getProductGroupId()));
            drug.setProductGroup(productGroup);
        }
        if (request.getDispensingUnitId() != null) {
            Unit dispensingUnit = unitRepository.findById(request.getDispensingUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + request.getDispensingUnitId()));
            drug.setDispensingUnit(dispensingUnit);
        }
        if (request.getMrp() != null) {
            drug.setMrp(request.getMrp());
        }
        if (request.getSalePrice() != null) {
            drug.setSalePrice(request.getSalePrice());
        }
        if (request.getPurchasePrice() != null) {
            drug.setPurchasePrice(request.getPurchasePrice());
        }
        if (request.getRackNo() != null) {
            drug.setRackNo(request.getRackNo());
        }
        if (request.getReminderStock() != null) {
            drug.setReminderStock(request.getReminderStock());
        }
        if (request.getHsnCode() != null) {
            drug.setHsnCode(request.getHsnCode());
        }
        if (request.getProductCode() != null) {
            drug.setProductCode(request.getProductCode());
        }
        if (request.getDepartmentId() != null) {
            drug.setDepartmentId(request.getDepartmentId());
        }

        Drug saved = drugRepository.save(drug);
        return toResponse(saved);
    }

    @Transactional
    public List<FormularyRuleResponse> upsertFormularyRules(UUID drugId, List<FormularyRuleRequest> requests) {
        Drug drug = drugRepository.findById(drugId)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + drugId));

        // Simple strategy: delete existing and recreate for this drug
        List<FormularyRule> existing = formularyRuleRepository.findByDrug(drug);
        formularyRuleRepository.deleteAll(existing);

        List<FormularyRule> created = requests.stream()
                .map(req -> toEntity(drug, req))
                .map(formularyRuleRepository::save)
                .collect(Collectors.toList());

        return created.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormularyRuleResponse> getFormularyRules(UUID drugId) {
        Drug drug = drugRepository.findById(drugId)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + drugId));
        List<FormularyRule> rules = formularyRuleRepository.findByDrug(drug);
        return rules.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Phase P3 WS-G — preferred alternative drugs from active formulary rules (for substitution UI).
     */
    @Transactional(readOnly = true)
    public List<DrugResponse> listFormularyAlternativeDrugs(UUID drugId) {
        Drug drug = drugRepository.findById(drugId)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + drugId));
        List<FormularyRule> rules = formularyRuleRepository.findByDrug(drug);
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        for (FormularyRule r : rules) {
            if (r.getPreferredAlternativeDrugIdsJson() == null || r.getPreferredAlternativeDrugIdsJson().isBlank()) {
                continue;
            }
            try {
                UUID[] parsed = objectMapper.readValue(r.getPreferredAlternativeDrugIdsJson(), UUID[].class);
                for (UUID u : parsed) {
                    if (u != null) {
                        ids.add(u);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Invalid preferred_alternative_drug_ids for formulary rule " + r.getId(), e);
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return drugRepository.findAllById(ids).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private DrugResponse toResponse(Drug entity) {
        return DrugResponse.builder()
                .id(entity.getId())
                .genericName(entity.getGenericName())
                .brandName(entity.getBrandName())
                .strength(entity.getStrength())
                .form(entity.getForm())
                .route(entity.getRoute())
                .packSize(entity.getPackSize())
                .unitOfMeasure(entity.getUnitOfMeasure())
                .therapeuticClassId(entity.getTherapeuticClassId())
                .active(entity.isActive())
                .controlledDrugFlag(entity.isControlledDrugFlag())
                .controlledProfileCode(entity.getControlledProfileCode())
                .batchRequired(entity.isBatchRequired())
                .expiryRequired(entity.isExpiryRequired())
                .manufacturerId(entity.getManufacturer().getId())
                .manufacturerName(entity.getManufacturer().getName())
                .productGroupId(entity.getProductGroup() != null ? entity.getProductGroup().getId() : null)
                .productGroupName(entity.getProductGroup() != null ? entity.getProductGroup().getName() : null)
                .dispensingUnitId(entity.getDispensingUnit() != null ? entity.getDispensingUnit().getId() : null)
                .dispensingUnitAbbreviation(entity.getDispensingUnit() != null ? entity.getDispensingUnit().getAbbreviation() : null)
                .mrp(entity.getMrp())
                .salePrice(entity.getSalePrice())
                .purchasePrice(entity.getPurchasePrice())
                .rackNo(entity.getRackNo())
                .reminderStock(entity.getReminderStock())
                .hsnCode(entity.getHsnCode())
                .productCode(entity.getProductCode())
                .departmentId(entity.getDepartmentId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static String normalizeProfileCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private FormularyRule toEntity(Drug drug, FormularyRuleRequest request) {
        String json = null;
        if (request.getPreferredAlternativeDrugIds() != null) {
            try {
                json = objectMapper.writeValueAsString(request.getPreferredAlternativeDrugIds());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid preferredAlternativeDrugIds", e);
            }
        }
        return FormularyRule.builder()
                .drug(drug)
                .restricted(request.getRestricted() != null && request.getRestricted())
                .restrictionReason(request.getRestrictionReason())
                .wardId(request.getWardId())
                .departmentId(request.getDepartmentId())
                .corporateContractId(request.getCorporateContractId())
                .preferredAlternativeDrugIdsJson(json)
                .build();
    }

    private FormularyRuleResponse toResponse(FormularyRule entity) {
        List<UUID> alternatives;
        if (entity.getPreferredAlternativeDrugIdsJson() != null) {
            try {
                UUID[] ids = objectMapper.readValue(entity.getPreferredAlternativeDrugIdsJson(), UUID[].class);
                alternatives = List.of(ids);
            } catch (Exception e) {
                alternatives = Collections.emptyList();
            }
        } else {
            alternatives = Collections.emptyList();
        }

        return FormularyRuleResponse.builder()
                .id(entity.getId())
                .drugId(entity.getDrug().getId())
                .restricted(entity.isRestricted())
                .restrictionReason(entity.getRestrictionReason())
                .wardId(entity.getWardId())
                .departmentId(entity.getDepartmentId())
                .corporateContractId(entity.getCorporateContractId())
                .preferredAlternativeDrugIds(alternatives)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

