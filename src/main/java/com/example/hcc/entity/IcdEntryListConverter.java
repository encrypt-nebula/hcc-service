package com.example.hcc.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Converter
public class IcdEntryListConverter implements AttributeConverter<List<IcdEntry>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<IcdEntry> attribute) {
        try {
            return attribute == null ? null : mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<IcdEntry> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || dbData.equals("null")) {
            return new ArrayList<>();
        }
        
        try {
            // Trim and handle potential double-escaping from some DB exports
            String json = dbData.trim();
            if (json.startsWith("\"") && json.endsWith("\"") && json.length() > 2) {
                json = mapper.readValue(json, String.class);
            }

            List<Object> rawList = mapper.readValue(json, new TypeReference<List<Object>>() {});
            List<IcdEntry> result = new ArrayList<>();
            
            for (Object item : rawList) {
                if (item instanceof String) {
                    // Legacy: ["A01"]
                    result.add(IcdEntry.builder().code((String) item).reasons(new ArrayList<>()).build());
                } else if (item instanceof Map) {
                    // New: [{"code":"A01"}]
                    result.add(mapper.convertValue(item, IcdEntry.class));
                }
            }
            return result;
        } catch (Exception e) {
            System.err.println("Failed to convert JSON from DB: " + dbData + ". Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
