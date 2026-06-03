package com.easyops.hospital.entity;

import com.easyops.hospital.dto.DoctorAppointmentSlot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class AppointmentSlotsConverter implements AttributeConverter<List<DoctorAppointmentSlot>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<DoctorAppointmentSlot>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<DoctorAppointmentSlot> attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<DoctorAppointmentSlot> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(dbData, TYPE_REF);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
