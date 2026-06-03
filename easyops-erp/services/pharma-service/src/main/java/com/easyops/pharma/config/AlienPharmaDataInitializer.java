package com.easyops.pharma.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Initializes Alien Pharma organization and product data at pharma-service startup.
 * Controlled by pharma.alien-pharma.init.enabled (default: false - to avoid wiping user roles on restart).
 *
 * Runs alien-pharma.sql via ScriptUtils (ScriptUtils-safe, no DO $$ blocks).
 * Verifies UOMs in admin.organization_app_data; if 0, runs Java fallback insert.
 */
@Component
@Slf4j
public class AlienPharmaDataInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final DataSource dataSource;
    private final boolean enabled;

    private volatile boolean initialized = false;

    public AlienPharmaDataInitializer(
            DataSource dataSource,
            @Value("${pharma.alien-pharma.init.enabled:false}") boolean enabled) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        if (initialized) {
            return;
        }
        if (!enabled) {
            log.info("Alien Pharma data initialization is DISABLED. Set pharma.alien-pharma.init.enabled=true to enable.");
            return;
        }

        try {
            log.info("Starting Alien Pharma data initialization...");

            // Run alien-pharma.sql via ScriptUtils (ScriptUtils-safe)
            log.info("Running alien-pharma.sql initialization script...");
            runScript("sql/alien-pharma.sql", true); // Continue on error

            ensureUomsInOrganizationAppData();
            log.info("Alien Pharma data initialization completed successfully");
            initialized = true;
        } catch (Exception e) {
            log.error("Failed to initialize Alien Pharma data: {}", e.getMessage(), e);
            try {
                log.info("Attempting fallback UOM insertion...");
                insertUOMsFallback();
            } catch (Exception fallbackError) {
                log.error("Fallback UOM insertion also failed", fallbackError);
            }
        }
    }



    private void runScript(String classpathPath) throws SQLException {
        runScript(classpathPath, false);
    }

    private void runScript(String classpathPath, boolean continueOnError) throws SQLException {
        ClassPathResource resource = new ClassPathResource(classpathPath);
        if (!resource.exists()) {
            log.warn("Script not found: {}", classpathPath);
            return;
        }
        EncodedResource encoded = new EncodedResource(resource, StandardCharsets.UTF_8);
        try (Connection c = dataSource.getConnection()) {
            boolean ac = c.getAutoCommit();
            c.setAutoCommit(true);
            try {
                ScriptUtils.executeSqlScript(c, encoded, false, continueOnError,
                        ScriptUtils.DEFAULT_COMMENT_PREFIX,
                        ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
                        ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                        ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
            } finally {
                c.setAutoCommit(ac);
            }
        }
        log.info("Executed script: {}", classpathPath);
    }

    /**
     * Verify UOMs exist; if not, run Java fallback insert.
     */
    private void ensureUomsInOrganizationAppData() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            try (var stmt = c.createStatement();
                 var rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM admin.organization_app_data " +
                     "WHERE type = 'UOM' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')")) {
                if (rs.next()) {
                    int cnt = rs.getInt("cnt");
                    if (cnt == 0) {
                        log.warn("No UOMs in organization_app_data; running fallback insert.");
                        insertUOMsFallback();
                    } else {
                        log.info("Alien Pharma UOMs in organization_app_data: {}", cnt);
                    }
                }
            }
        }
    }

    private void insertUOMsFallback() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (var st = c.createStatement();
                     var rs = st.executeQuery("SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'")) {
                    if (!rs.next()) {
                        log.warn("Alien Pharma organization not found; cannot insert UOMs.");
                        return;
                    }
                }

                try (var st = c.createStatement()) {
                    int n = st.executeUpdate(
                        "INSERT INTO admin.organization_app_data (" +
                        "id, organization_id, type, code, name, description, extra_attributes, " +
                        "is_active, display_order, created_at, updated_at, created_by, updated_by" +
                        ") SELECT gen_random_uuid(), (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), " +
                        "'UOM', u.code, u.name, u.description, u.extra_attributes::jsonb, " +
                        "TRUE, u.display_order, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system' " +
                        "FROM (VALUES " +
                        "('GRAM', 'Gram', 'Gram (g) unit of mass', '{\"category\":\"MASS\"}', 1), " +
                        "('ML', 'Milliliter', 'Milliliter (ml) unit of volume', '{\"category\":\"VOLUME\"}', 2), " +
                        "('PCS', 'Pieces', 'Pieces (pcs) unit of count', '{\"category\":\"COUNT\"}', 3)" +
                        ") AS u(code, name, description, extra_attributes, display_order) " +
                        "ON CONFLICT (organization_id, type, code) DO UPDATE SET " +
                        "name = EXCLUDED.name, description = EXCLUDED.description, " +
                        "extra_attributes = EXCLUDED.extra_attributes, is_active = EXCLUDED.is_active, " +
                        "display_order = EXCLUDED.display_order, updated_at = CURRENT_TIMESTAMP");
                    c.commit();
                    log.info("Fallback UOM insert completed; rows affected: {}", n);
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
