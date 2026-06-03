package com.easyops.hospital.entity;

import com.easyops.hospital.dto.DoctorWeeklySchedule;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class WeeklyScheduleConverter implements AttributeConverter<DoctorWeeklySchedule, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(DoctorWeeklySchedule attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public DoctorWeeklySchedule convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return MAPPER.readValue(dbData, DoctorWeeklySchedule.class);
        } catch (Exception e) {
            return null;
        }
    }
}
