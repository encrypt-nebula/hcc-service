package com.example.hcc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ExtractDataRequest {

    @NotBlank(message = "fileName is required")
    private String fileName;

    @NotBlank(message = "s3Path is required")
    private String s3Path;

    @NotNull(message = "totalPages is required")
    private Integer totalPages;

    private String signature;

    private String credentials;

    @NotBlank(message = "projectName is required")
    private String projectName;

    @NotBlank(message = "projectType is required")
    private String projectType;

    private LocalDate dos;
    private LocalDate dob;
    private Integer pageStart;
    private Integer pageEnd;

    @NotBlank(message = "firstName is required")
    private String firstName;

    @NotBlank(message = "lastName is required")
    private String lastName;

    @NotNull(message = "projectId is required")
    private Long projectId;

    @NotBlank(message = "workUnitType is required")
    private String workUnitType;

    private String dbStatus;

    @Valid
    private List<ExtractDetail> details;

    @Data
    public static class ExtractDetail {
        private LocalDate dos;
        private List<String> extractedIcdCodes;
        private List<String> aiSuggestedIcdCode;
    }
}
