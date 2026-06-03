package com.easyops.users.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to fetch enabled frontend modules from system.settings.
 * Modules are enabled by Liquibase when respective services (pharma, hospital) start.
 */
@Service
public class ModuleConfigService {

    private static final String MODULES_QUERY =
            "SELECT key FROM system.settings WHERE category = 'modules' AND value = 'true'";

    private final JdbcTemplate jdbcTemplate;

    public ModuleConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns list of enabled module keys (e.g., pharma, hospital, dashboard).
     * Keys in DB are stored as 'modules.pharma' - we strip the 'modules.' prefix.
     */
    public List<String> getEnabledModules() {
        List<String> keys = jdbcTemplate.query(MODULES_QUERY,
                (rs, rowNum) -> rs.getString("key"));

        return keys.stream()
                .map(k -> k != null && k.startsWith("modules.") ? k.substring(8) : k)
                .collect(Collectors.toList());
    }
}
