package com.easyops.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateVariableSchemaValidatorTest {

    private final TemplateVariableSchemaValidator validator = new TemplateVariableSchemaValidator(new ObjectMapper());

    @Test
    void validateSchemaJson_acceptsRequiredAndAllowedArrays() {
        assertThatCode(() -> validator.validateSchemaJson("""
                {"required":["patientName"],"allowed":["patientName","appointmentDate"]}
                """)).doesNotThrowAnyException();
    }

    @Test
    void validateTemplateDefinition_rejectsUndeclaredPlaceholder() {
        assertThatThrownBy(() -> validator.validateTemplateDefinition(
                "SMS",
                null,
                "Hello {{patientName}} {{doctorName}}",
                "{\"required\":[\"patientName\"]}"
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Placeholder variable is not declared in variablesSchema: doctorName");
    }

    @Test
    void validatePayloadAgainstSchema_rejectsUnexpectedVariable() {
        assertThatThrownBy(() -> validator.validatePayloadAgainstSchema(
                "{\"required\":[\"patientName\"],\"allowed\":[\"patientName\"]}",
                Map.of("patientName", "John", "extra", "x")
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unexpected variable supplied: extra");
    }
}
