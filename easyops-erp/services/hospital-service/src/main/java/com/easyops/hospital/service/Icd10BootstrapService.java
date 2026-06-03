package com.easyops.hospital.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class Icd10BootstrapService implements ApplicationRunner {

    private static final int BATCH_SIZE = 500;
    private static final String DEFAULT_CLASSPATH_PATH = "reference-data/icd102019en.xml";

    private final JdbcTemplate jdbcTemplate;

    @Value("${hospital.icd10.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${hospital.icd10.bootstrap.xml-path:}")
    private String configuredXmlPath;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("ICD-10 bootstrap is disabled by configuration");
            return;
        }

        try {
            Icd10ImportData importData;
            String source;

            ClassPathResource classPathResource = new ClassPathResource(DEFAULT_CLASSPATH_PATH);
            if (classPathResource.exists()) {
                importData = parseClaml(classPathResource.getInputStream());
                source = "classpath:" + DEFAULT_CLASSPATH_PATH;
            } else {
                Optional<Path> xmlPath = resolveXmlPath();
                if (xmlPath.isEmpty()) {
                    log.warn("ICD-10 XML file not found in classpath and no valid configured path was provided");
                    return;
                }
                importData = parseClaml(xmlPath.get());
                source = xmlPath.get().toString();
            }

            if (importData.categories().isEmpty()) {
                log.warn("ICD-10 bootstrap parsed zero category records from {}", source);
                return;
            }

            // Ensure bootstrap source is authoritative: replace any seed/partial data with the full import.
            jdbcTemplate.execute("TRUNCATE TABLE ehr.icd10_codes");
            int inserted = upsertIcd10Categories(importData.categories());
            log.info("ICD-10 bootstrap loaded {} records from {}", inserted, source);
        } catch (XMLStreamException | IOException e) {
            log.error("Failed to bootstrap ICD-10 data from XML", e);
        }
    }

    private Optional<Path> resolveXmlPath() {
        List<Path> candidates = new ArrayList<>();

        if (configuredXmlPath != null && !configuredXmlPath.isBlank()) {
            candidates.add(Path.of(configuredXmlPath).toAbsolutePath().normalize());
        }

        Set<Path> seen = new HashSet<>();
        for (Path candidate : candidates) {
            if (!seen.add(candidate)) {
                continue;
            }
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Icd10ImportData parseClaml(Path xmlPath) throws XMLStreamException, IOException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(xmlPath))) {
            return parseClaml(inputStream);
        }
    }

    private Icd10ImportData parseClaml(InputStream sourceInputStream) throws XMLStreamException, IOException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        Map<String, ClassNode> allClasses = new HashMap<>();
        List<ClassNode> categories = new ArrayList<>();

        try (InputStream inputStream = new BufferedInputStream(sourceInputStream)) {
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            ClassNode current = null;
            boolean inPreferredRubric = false;
            boolean inLabel = false;
            StringBuilder labelBuilder = new StringBuilder();

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();

                    switch (localName) {
                        case "Class" -> current = new ClassNode(
                                reader.getAttributeValue(null, "code"),
                                reader.getAttributeValue(null, "kind")
                        );
                        case "SuperClass" -> {
                            if (current != null) {
                                current.superCode = reader.getAttributeValue(null, "code");
                            }
                        }
                        case "Rubric" -> {
                            if (current != null) {
                                inPreferredRubric = "preferred".equals(reader.getAttributeValue(null, "kind"));
                            }
                        }
                        case "Label" -> {
                            if (current != null && inPreferredRubric) {
                                inLabel = true;
                                labelBuilder.setLength(0);
                            }
                        }
                        default -> {
                            // no-op
                        }
                    }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (current != null && inLabel) {
                        labelBuilder.append(reader.getText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();

                    if ("Label".equals(localName) && current != null && inLabel) {
                        String label = normalizeWhitespace(labelBuilder.toString());
                        if (!label.isBlank() && current.preferredLabel == null) {
                            current.preferredLabel = label;
                        }
                        inLabel = false;
                    } else if ("Rubric".equals(localName)) {
                        inPreferredRubric = false;
                    } else if ("Class".equals(localName) && current != null) {
                        allClasses.put(current.code, current);
                        if ("category".equals(current.kind) && current.preferredLabel != null && !current.preferredLabel.isBlank()) {
                            categories.add(current);
                        }
                        current = null;
                        inPreferredRubric = false;
                        inLabel = false;
                        labelBuilder.setLength(0);
                    }
                }
            }
            reader.close();
        }

        for (ClassNode category : categories) {
            category.categoryLabel = resolveAncestorLabel(category, "block", allClasses);
            category.chapterLabel = resolveAncestorLabel(category, "chapter", allClasses);
        }

        return new Icd10ImportData(categories);
    }

    private String resolveAncestorLabel(ClassNode node, String ancestorKind, Map<String, ClassNode> allClasses) {
        String parentCode = node.superCode;
        Set<String> visited = new HashSet<>();

        while (parentCode != null && !parentCode.isBlank() && visited.add(parentCode)) {
            ClassNode parent = allClasses.get(parentCode);
            if (parent == null) {
                return null;
            }
            if (ancestorKind.equals(parent.kind)) {
                return parent.preferredLabel;
            }
            parentCode = parent.superCode;
        }
        return null;
    }

    private int upsertIcd10Categories(List<ClassNode> categories) {
        final int[] total = {0};

        for (int i = 0; i < categories.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, categories.size());
            List<ClassNode> batch = categories.subList(i, end);

            jdbcTemplate.batchUpdate(
                    """
                    INSERT INTO ehr.icd10_codes (code, description, category, chapter, is_valid)
                    VALUES (?, ?, ?, ?, TRUE)
                    ON CONFLICT (code) DO UPDATE
                    SET description = EXCLUDED.description,
                        category = EXCLUDED.category,
                        chapter = EXCLUDED.chapter,
                        is_valid = TRUE,
                        updated_at = CURRENT_TIMESTAMP
                    """,
                    batch,
                    batch.size(),
                    (ps, row) -> {
                        ps.setString(1, row.code);
                        ps.setString(2, row.preferredLabel);
                        ps.setString(3, row.categoryLabel);
                        ps.setString(4, row.chapterLabel);
                    }
            );
            total[0] += batch.size();
        }

        return total[0];
    }

    private String normalizeWhitespace(String input) {
        return input == null ? "" : input.replaceAll("\\s+", " ").trim();
    }

    private record Icd10ImportData(List<ClassNode> categories) {
    }

    private static final class ClassNode {
        private final String code;
        private final String kind;
        private String superCode;
        private String preferredLabel;
        private String categoryLabel;
        private String chapterLabel;

        private ClassNode(String code, String kind) {
            this.code = code;
            this.kind = kind;
        }
    }
}
