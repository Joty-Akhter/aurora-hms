package com.easyops.communication.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void render_replacesAllPlaceholdersDeterministically() {
        String rendered = renderer.render(
                "Hello {{patientName}}, appointment at {{slot}}",
                Map.of("patientName", "John", "slot", "10:00")
        );
        assertThat(rendered).isEqualTo("Hello John, appointment at 10:00");
    }

    @Test
    void render_failsFastWhenRequiredVariableMissing() {
        assertThatThrownBy(() -> renderer.render("Hello {{patientName}} {{doctorName}}", Map.of("patientName", "John")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing required placeholder variable: doctorName");
    }
}
