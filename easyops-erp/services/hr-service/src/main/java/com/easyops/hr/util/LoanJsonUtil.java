package com.easyops.hr.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class LoanJsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LoanJsonUtil() {
    }

    public static List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public static String toJsonStringList(List<String> list) {
        if (list == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize list to JSON", e);
        }
    }
}
