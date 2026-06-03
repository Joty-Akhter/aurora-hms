package com.easyops.users.controller;

import com.easyops.users.service.ModuleConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public API for frontend module configuration.
 * Returns enabled modules from database (set by Liquibase when services start).
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ModuleConfigController {

    private final ModuleConfigService moduleConfigService;

    public ModuleConfigController(ModuleConfigService moduleConfigService) {
        this.moduleConfigService = moduleConfigService;
    }

    /**
     * GET /api/config/modules
     * Returns enabled module keys for the frontend. No auth required.
     */
    @GetMapping("/modules")
    public ResponseEntity<Map<String, List<String>>> getEnabledModules() {
        List<String> enabledModules = moduleConfigService.getEnabledModules();
        return ResponseEntity.ok(Map.of("enabledModules", enabledModules));
    }
}
