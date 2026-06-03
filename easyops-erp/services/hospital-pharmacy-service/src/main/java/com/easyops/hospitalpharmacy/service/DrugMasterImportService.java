package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.Manufacturer;
import com.easyops.hospitalpharmacy.repository.DrugRepository;
import com.easyops.hospitalpharmacy.repository.ManufacturerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrugMasterImportService {

    private final ManufacturerRepository manufacturerRepository;
    private final DrugRepository drugRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void importFromJson(Path jsonPath) {
        if (jsonPath == null || !Files.exists(jsonPath)) {
            log.warn("Drug master import skipped – file does not exist: {}", jsonPath);
            return;
        }

        log.info("Starting drug master import from JSON: {}", jsonPath);
        try {
            List<BrandRecord> records = objectMapper.readValue(
                    Files.newBufferedReader(jsonPath),
                    new TypeReference<List<BrandRecord>>() {}
            );

            if (records.isEmpty()) {
                log.info("Drug master import: no records found in JSON file {}", jsonPath);
                return;
            }

            Map<String, Manufacturer> manufacturerCache = new HashMap<>();
            long manufacturerCreated = 0;
            long drugsCreated = 0;

            for (BrandRecord record : records) {
                if (record.brandName == null || record.brandName.isBlank()) {
                    continue;
                }
                if (record.genericName == null || record.genericName.isBlank()) {
                    continue;
                }
                if (record.companyName == null || record.companyName.isBlank()) {
                    continue;
                }

                String companyKey = record.companyName.trim().toLowerCase();
                Manufacturer manufacturer = manufacturerCache.get(companyKey);
                if (manufacturer == null) {
                    manufacturer = manufacturerRepository.findByNameContainingIgnoreCase(record.companyName)
                            .stream()
                            .filter(m -> m.getName().equalsIgnoreCase(record.companyName.trim()))
                            .findFirst()
                            .orElseGet(() -> {
                                Manufacturer m = Manufacturer.builder()
                                        .name(record.companyName.trim())
                                        .country(null)
                                        .contactInfo(null)
                                        .active(true)
                                        .build();
                                return manufacturerRepository.save(m);
                            });
                    if (manufacturer.getCreatedAt() == null) {
                        manufacturer.setCreatedAt(OffsetDateTime.now());
                        manufacturer.setUpdatedAt(OffsetDateTime.now());
                    }
                    manufacturerCache.put(companyKey, manufacturer);
                    manufacturerCreated++;
                }

                // Avoid creating obvious duplicates for same brand, strength, form, manufacturer
                final Manufacturer existingManufacturer = manufacturer;
                boolean exists = drugRepository.findByGenericNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(
                                record.genericName.trim(),
                                record.brandName.trim())
                        .stream()
                        .anyMatch(d ->
                                d.getBrandName() != null &&
                                d.getBrandName().equalsIgnoreCase(record.brandName.trim()) &&
                                Objects.equals(d.getStrength(), record.dose) &&
                                Objects.equals(d.getForm(), record.doseForm) &&
                                d.getManufacturer() != null &&
                                d.getManufacturer().getId().equals(existingManufacturer.getId())
                        );
                if (exists) {
                    continue;
                }

                Drug drug = Drug.builder()
                        .genericName(record.genericName.trim())
                        .brandName(record.brandName.trim())
                        .strength(record.dose)
                        .form(record.doseForm)
                        .route(null)
                        .packSize(null)
                        .unitOfMeasure(null)
                        .therapeuticClassId(null)
                        .active(true)
                        .controlledDrugFlag(false)
                        .batchRequired(true)
                        .expiryRequired(true)
                        .manufacturer(manufacturer)
                        .build();

                drugRepository.save(drug);
                drugsCreated++;
            }

            log.info("Drug master import completed. Manufacturers created or reused: {}, Drugs created: {}",
                    manufacturerCreated, drugsCreated);
        } catch (IOException e) {
            log.error("Failed to import drug master from JSON {}", jsonPath, e);
            throw new IllegalStateException("Failed to import drug master from JSON", e);
        }
    }

    // Simple DTO matching scraped JSON structure
    private static class BrandRecord {
        public Integer page;
        public String brandName;
        public String dose;
        public String doseForm;
        public String genericName;
        public String companyName;
        public String scrapedAt;
    }
}

