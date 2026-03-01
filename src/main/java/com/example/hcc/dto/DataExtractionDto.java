package com.example.hcc.dto;

import lombok.Data;
import java.util.List;

@Data
public class DataExtractionDto {
    private String fileName;
    private String s3Path;
    private Integer totalPages;
    private String signature;
    private String credentials;
    private String projectName;
    private String projectType;
    private String dos;
    private String dob;
    private String firstName;
    private String lastName;
    private Long projectId;
    private String workUnitType;
    private List<EncounterDetailDto> details;
    private String dbStatus;

    @Data
    public static class EncounterDetailDto {
        private String dos;
        private List<String> extractedIcdCodes;
        private List<String> aiSuggestedIcdCode;
    }
}
