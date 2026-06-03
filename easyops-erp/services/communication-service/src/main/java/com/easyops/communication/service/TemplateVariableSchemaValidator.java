package com.easyops.communication.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateVariableSchemaValidator {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");
    private final ObjectMapper objectMapper;

    public TemplateVariableSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateSchemaJson(String variablesSchema) {
        JsonNode schema = parseSchema(variablesSchema);
        JsonNode requiredNode = schema.get("required");
        if (requiredNode != null && !requiredNode.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "variablesSchema.required must be an array");
        }
        JsonNode allowedNode = schema.get("allowed");
        if (allowedNode != null && !allowedNode.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "variablesSchema.allowed must be an array");
        }
    }

    public void validateTemplateDefinition(String channel, String subjectTemplate, String bodyTemplate, String variablesSchema) {
        if ("SMS".equalsIgnoreCase(channel) && subjectTemplate != null && !subjectTemplate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMS template cannot define subjectTemplate");
        }
        if ("EMAIL".equalsIgnoreCase(channel) && (subjectTemplate == null || subjectTemplate.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMAIL template requires subjectTemplate");
        }
        if ("SMS".equalsIgnoreCase(channel) && bodyTemplate != null && bodyTemplate.length() > 1600) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMS template body exceeds 1600 characters");
        }

        Set<String> allowedVariables = allowedVariables(parseSchema(variablesSchema));
        Set<String> placeholders = extractPlaceholders(subjectTemplate, bodyTemplate);
        for (String placeholder : placeholders) {
            if (!allowedVariables.contains(placeholder)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Placeholder variable is not declared in variablesSchema: " + placeholder
                );
            }
        }
    }

    public void validatePayloadAgainstSchema(String variablesSchema, Map<String, Object> payload) {
        JsonNode schema = parseSchema(variablesSchema);
        JsonNode requiredNode = schema.get("required");
        Set<String> allowed = allowedVariables(schema);

        if (requiredNode != null) {
            Iterator<JsonNode> iterator = requiredNode.elements();
            while (iterator.hasNext()) {
                String key = iterator.next().asText();
                if (payload == null || !payload.containsKey(key) || payload.get(key) == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required variable: " + key);
                }
            }
        }

        if (payload != null) {
            for (String key : payload.keySet()) {
                if (!allowed.contains(key)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected variable supplied: " + key);
                }
            }
        }
    }

    private JsonNode parseSchema(String variablesSchema) {
        try {
            return objectMapper.readTree(variablesSchema);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "variablesSchema must be valid JSON");
        }
    }

    private Set<String> allowedVariables(JsonNode schema) {
        Set<String> allowed = new HashSet<>();
        JsonNode allowedNode = schema.get("allowed");
        if (allowedNode != null && allowedNode.isArray()) {
            Iterator<JsonNode> iterator = allowedNode.elements();
            while (iterator.hasNext()) {
                allowed.add(iterator.next().asText());
            }
        }
        JsonNode requiredNode = schema.get("required");
        if (requiredNode != null && requiredNode.isArray()) {
            Iterator<JsonNode> iterator = requiredNode.elements();
            while (iterator.hasNext()) {
                allowed.add(iterator.next().asText());
            }
        }
        if (allowed.isEmpty() && schema.isObject()) {
            Iterator<String> fields = schema.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!"required".equals(field) && !"allowed".equals(field)) {
                    allowed.add(field);
                }
            }
        }
        return allowed;
    }

    private Set<String> extractPlaceholders(String... templates) {
        Set<String> placeholders = new HashSet<>();
        for (String template : templates) {
            if (template == null || template.isBlank()) {
                continue;
            }
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
            while (matcher.find()) {
                placeholders.add(matcher.group(1));
            }
        }
        return placeholders;
    }
}
