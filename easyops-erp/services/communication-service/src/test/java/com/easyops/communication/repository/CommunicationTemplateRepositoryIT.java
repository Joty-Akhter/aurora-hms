package com.easyops.communication.repository;

import com.easyops.communication.entity.CommunicationTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CommunicationTemplateRepositoryIT {

    @Autowired
    private CommunicationTemplateRepository repository;

    @Test
    void saveAndFindByUniqueKeyFields() {
        CommunicationTemplate entity = new CommunicationTemplate();
        entity.setTemplateKey("appointment.confirmation");
        entity.setChannel("SMS");
        entity.setLocale("en");
        entity.setVersion(1);
        entity.setStatus("ACTIVE");
        entity.setBodyTemplate("Hello {{patientName}}");
        entity.setVariablesSchema("{\"patientName\":\"string\"}");
        entity.setCreatedBy("integration-test");

        CommunicationTemplate saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findByTemplateKeyAndChannelAndLocaleAndVersion(
                "appointment.confirmation", "SMS", "en", 1)).isPresent();
    }
}
