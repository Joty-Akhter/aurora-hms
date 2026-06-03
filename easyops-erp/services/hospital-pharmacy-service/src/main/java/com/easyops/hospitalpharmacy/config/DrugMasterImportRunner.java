package com.easyops.hospitalpharmacy.config;

import com.easyops.hospitalpharmacy.service.DrugMasterImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrugMasterImportRunner implements CommandLineRunner {

    private final DrugMasterImportService importService;

    @Value("${hospital.pharmacy.import.drug-master-json-path:}")
    private String jsonPath;

    @Override
    public void run(String... args) {
        if (jsonPath == null || jsonPath.isBlank()) {
            log.info("Drug master import path not configured; skipping import.");
            return;
        }

        try {
            importService.importFromJson(Path.of(jsonPath));
        } catch (Exception e) {
            log.error("Drug master import failed from path {}", jsonPath, e);
        }
    }
}

